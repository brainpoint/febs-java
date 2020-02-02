/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import cn.brainpoint.febs.libs.promise.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;


/**
 * 可以使用模板的方式,指定第一次then的参数类型.
 * <p>
 * Example:
 * <p>
 * promiseObj
 * .then(...)
 * .then(...)
 * .fail(...)
 * .execute();
 *
 * @Author pengxiang.li
 * @Date 2020/1/30 5:12 下午
 */
public class Promise<TP> implements java.lang.Comparable, IPromise {
    static public final String STATUS_PENDING = "pending";
    static public final String STATUS_FULFILLED = "fulfilled";
    static public final String STATUS_REJECTED = "rejected";
    static private ConcurrentSkipListSet<Object> globalObjectSet = new ConcurrentSkipListSet<>();
    static private IReject globalUncaughtExceptionHandler;

    private Runnable onSuccessListener2;
    private IResolve<TP> onSuccessListener;
    private IReject onErrorListener;
    private IFinish onFinishListener;
    private IExecute<TP> onExecuteListener;
    private Promise<?> child;
    private String status = STATUS_PENDING;
    private Object tag;
    private Promise ancestor;
    private boolean _inExecute = false;
    private Object _inTag;
    private CompletableFuture _cf;

    private static class PromiseExecutor<TP> implements IPromise {
        private Promise<TP> p;

        public PromiseExecutor(Promise<TP> p) {
            this.p = p;
        }

        @Override
        public IPromise execute() {
            this.p.execute();
            return this;
        }

        @Override
        public String getStatus() {
            return this.p.getStatus();
        }

        @Override
        public Object getTag() {
            return this.p.getTag();
        }

        @Override
        public void setTag(Object tag) {
            this.p.setTag(tag);
        }
    }

    /**
     * 未设置fail的promise, 如果发生异常, 将由此处理异常.
     * @param listener
     */
    public static void setUncaughtExceptionHandler(IReject listener) {
        globalUncaughtExceptionHandler = listener;
    }

    /**
     * Promise.all({})
     * .then(...)
     *
     * @param list
     * @return Promise
     */
    public static Promise all(Promise... list) {

        if (list == null || list.length <= 0) {
            throw new RuntimeException("Promise list should not be empty!");
        }

        if (list != null && list.length > 0) {
            Promise p = new Promise((resolve, reject)->{
                resolve.execute(null);
            }).then(new Runnable() {
                int completedCount = 0;
                Object[] result = new Object[list.length];
                Exception ex;

                @Override
                public void run() {
                    for (int i = 0; i < list.length; i++) {
                        Promise promise = list[i];
                        promise._inTag = i;
                        promise.then(res -> {
                            result[(int) promise._inTag] = res;
                            completed(null);
                            return res;
                        }).fail(this::completed);
                    }
                    Promise.join(5, list);
                    if (ex != null) {
                        throw new RuntimeException(ex);
                    }
                }

                private Object completed(Exception err) throws Exception {
                    completedCount++;
                    if (err != null) {
                        ex = err;
                    }
                    return null;
                }
            });
            return p;
        } else {
            try {
                Promise p = new Promise((Promise)null);
                p.resolve(new ArrayList<>());
                return p;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * wait all promise done. will broke thread.
     *
     * @param list
     */
    public static void join(IPromise... list) {
        join(10, list);
    }

    /**
     * wait all promise done. will broke thread.
     *
     * @param list
     * @param peekInMillisecond 查询结果的间隔.
     */
    public static void join(int peekInMillisecond, IPromise... list) {
        while (true) {
            int doneCount = 0;
            for (int i = 0; i < list.length; i++) {
                if (list[i].getStatus() != STATUS_PENDING) {
                    doneCount++;
                }
                else if (list[i] instanceof PromiseExecutor) {
                    PromiseExecutor pe = (PromiseExecutor)list[i];
                    Promise p = pe.p;
                    p = p.ancestor == null? p: p.ancestor;
                    if (!p._inExecute) {
                        p.execute();
                    }
                } else {
                    Promise p = (Promise)list[i];
                    p = p.ancestor == null? p: p.ancestor;
                    if (!p._inExecute) {
                        p.execute();
                    }
                }
            }
            if (doneCount == list.length) {
                return;
            }
            try {
                Thread.sleep(peekInMillisecond);
            } catch (Exception e) {

            }
        }
    }

    /**
     * 当前的状态.
     *
     * @return
     */
    @Override
    public String getStatus() {
        return this.ancestor == null ? this.status : this.ancestor.status;
    }

    @Override
    public Object getTag() {
        return this.ancestor == null ? this.tag : this.ancestor.tag;
    }

    @Override
    public void setTag(Object tag) {
        if (this.ancestor == null) {
            this.tag = tag;
        } else {
            this.ancestor.tag = tag;
        }
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return 1;
        }
        int h1 = this.hashCode();
        int h2 = o.hashCode();
        return h1 == h2 ? 0 : (h1 > h2 ? 1 : -1);
    }

    private Promise(Promise ancestor) {
        this.ancestor = ancestor;
    }

    /**
     * new Promise((IResolve resolve, IReject reject)-> { resolve.execute(...); });
     *
     * @param listener
     */
    public Promise(IExecute<TP> listener) {
        globalObjectSet.add(this);
        this.onExecuteListener = listener;
    }

    /**
     * execute the promise.
     */
    @Override
    public IPromise execute() {
        Promise ancestor = this.ancestor == null ? this : this.ancestor;

        if (!ancestor.status.equals(STATUS_PENDING) || ancestor._inExecute == true) {
            throw new RuntimeException("Promise is not in pending status");
        } else {
            ancestor._inExecute = true;

            try {
                ancestor._cf = CompletableFuture.supplyAsync(() -> {
                    try {
                        ancestor.onExecuteListener.execute(res -> {
                            ancestor.resolve(res);
                            return null;
                        }, e -> {
                            ancestor.reject(e);
                            return null;
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }, Febs.getExecutorService())
                        .handle((res, e) -> {
                            globalObjectSet.remove(ancestor);
                            if (e != null) {
                                try {
                                    ancestor.reject((Exception) e);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                } finally {
                                    // release memory
                                    Promise p = ancestor;
                                    p.status = STATUS_REJECTED;
                                    do {
                                        p._cf = null;
                                        Promise p1 = p.child;
                                        p.child = null;
                                        p = p1;
                                    } while (p != null);
                                }
                            }
                            // 获取最终的结果.
                            else {
                                Promise p = ancestor;
                                if (p.getStatus() == STATUS_PENDING) {
                                    p.status = STATUS_FULFILLED;
                                }
                                do {
                                    p._cf = null;
                                    Promise p1 = p.child;
                                    p.child = null;
                                    p = p1;
                                } while (p != null);
                            }
                            return null;
                        });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return this;
    }

    /**
     * After executing asynchronous function the result will be available in the success listener
     * as argument.
     *
     * @param listener IResolve
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise then(IResolve<TP> listener) {
        onSuccessListener2 = null;
        onSuccessListener = listener;
        child = new Promise(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * Use runable to executing asynchronous function. It cann't catch the return value of pre-chain.
     *
     * @param runnable
     * @return
     */
    public Promise then(Runnable runnable) {
        onSuccessListener2 = runnable;
        onSuccessListener = null;
        child = new Promise(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` chain, any `reject()` occurs in
     * previous async execution this function will be called.
     *
     * @param listener
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise fail(IReject listener) {
        onErrorListener = listener;
        child = new Promise(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` or `fail()` chain.
     */
    public IPromise finish(IFinish listener) {
        onFinishListener = listener;
        return new PromiseExecutor<TP>(this);
    }

    /**
     * Call this function with your resultant value, it will be available
     * in following `then()` function call.
     *
     * @param object your resultant value (any type of data you can pass as argument
     *               e.g. int, String, List, Map, any Java object)
     * @return This will return the resultant value you passed in the function call
     */
    private void resolve(Object object) throws Exception {
        // status = STATUS_FULFILLED;
        handleSuccess(object);
    }

    /**
     * Call this function with your error value, it will be available
     * in following `fail()` function call.
     *
     * @param object your error value (any type of data you can pass as argument
     *               e.g. int, String, List, Map, any Java object)
     */
    private void reject(Exception object) throws Exception {
        // status = STATUS_REJECTED;
        handleError(object);
    }

    private Object _handleSuccessAsyncRun(Object param) throws RuntimeException {
        try {
            if (onSuccessListener != null) {
                Object res = onSuccessListener.execute((TP) param);
                _handleSuccessAsyncAccept(res);
            } else if (onSuccessListener2 != null) {
                onSuccessListener2.run();
                _handleSuccessAsyncAccept(null);
            } else if (child != null) {
                child._handleSuccessAsyncRun(param);
            } else if (onFinishListener != null) {
                onFinishListener.execute();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void _handleSuccessAsyncAccept(Object res) throws RuntimeException {
        if (res != null) {
            if (res instanceof Promise) {
                if (child != null) {
                    Promise p = (Promise) res;
                    Promise pChild = p;
                    while (pChild.child != null) {
                        pChild = pChild.child;
                    }

                    Promise oldP = child;
                    child = p;
                    pChild.child = oldP;
                } else {
                    child = (Promise) res;
                }

                try {
                    child.resolve(res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (child != null) {
                try {
                    child.resolve(res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (onFinishListener != null) {
                onFinishListener.execute();
            }
        } else {
            if (child != null) {
                try {
                    child.resolve(res);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (onFinishListener != null) {
                onFinishListener.execute();
            }
        }
    }

    private void handleSuccess(Object object) throws Exception {
        // 在异步线程中, 不重新启动新线程.
        this._handleSuccessAsyncRun(object);
    }

    private void handleError(java.lang.Exception object) throws Exception {
        if (onErrorListener != null) {
            Object res = null;
            try {
                res = onErrorListener.execute(object);
            } catch (Exception e) {
                if (child != null) {
                    child.reject(object);
                } else if (onFinishListener != null) {
                    onFinishListener.execute();
                }
            }

            if (res != null) {
                if (res instanceof Promise) {
                    if (child != null) {
                        Promise p = (Promise) res;
                        Promise pChild = p;
                        while (pChild.child != null) {
                            pChild = pChild.child;
                        }

                        Promise oldP = child;
                        child = p;
                        pChild.child = oldP;
                    } else {
                        child = (Promise) res;
                    }
                    child.resolve(null);
                } else if (child != null) {
                    child.resolve(res);
                } else if (onFinishListener != null) {
                    onFinishListener.execute();
                }
            } else {
                if (child != null) {
                    child.resolve(null);
                } else if (onFinishListener != null) {
                    onFinishListener.execute();
                }
            }
        } else if (child != null) {
            child.reject(object);
        } else if (onFinishListener != null) {
            if (globalUncaughtExceptionHandler != null) {
                globalUncaughtExceptionHandler.execute(new FebsException("Promise uncaught exception", object));
            }

            onFinishListener.execute();
        }
    }
}

