/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.brainpoint.febs.libs.promise.IExecute;
import cn.brainpoint.febs.libs.promise.IFinish;
import cn.brainpoint.febs.libs.promise.IPromise;
import cn.brainpoint.febs.libs.promise.IReject;
import cn.brainpoint.febs.libs.promise.IRejectNoRet;
import cn.brainpoint.febs.libs.promise.IResolve;
import cn.brainpoint.febs.libs.promise.IResolveNoRet;


/**
 * The Promise utility.
 *
 * <i>e.g.</i>
 * <code>
 *  new Promise((resolve, reject)-&gt;{  resolve.execute();  })
 *    .then(()-&gt;{})
 *    .then(()-&gt;{})
 *    .fail((e)-&gt;{})
 *    .execute();
 * </code>
 *
 * @author pengxiang.li
 * @date  2020/1/30 5:12 下午
 */
public class Promise<TP> implements java.lang.Comparable<TP>, IPromise {

    static {
        Febs.init();
    }

    static public final String STATUS_PENDING = "pending";
    static public final String STATUS_FULFILLED = "fulfilled";
    static public final String STATUS_REJECTED = "rejected";
    static private ConcurrentSkipListSet<Object> globalObjectSet = new ConcurrentSkipListSet<Object>();
    static private IReject globalUncaughtExceptionHandler;

    private Runnable onSuccessListenerRunnable;
    private IResolve<TP> onSuccessListener;
    private IResolveNoRet<TP> onSuccessListenerNoRet;
    private IReject onErrorListener;
    private IRejectNoRet onErrorListenerNoRet;

    private IFinish onFinishListener;
    private IExecute<TP> onExecuteListener;
    private Promise<?> child;
    private String status = STATUS_PENDING;
    private Object tag;
    private Promise<?> ancestor;
    private boolean _inExecute = false;
    private Object _inTag;  // internal use.
    private CompletableFuture _cf;

    private static class PromiseExecutor<TP> implements IPromise {
        private Promise<TP> p;

        public PromiseExecutor(Promise<TP> p) {
            this.p = p;
        }

        @Override
        public boolean isExecutor() { return true; }

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

    public static String _dumpDebug() {
        return "globalObjectSet: " + globalObjectSet.size();
    }

    /**
     * Some promise object will catch exception use this method, if it have't call .fail()
     * <i>e.g.</i>
     * <code>
     *    Promise.setUncaughtExceptionHandler((e)-&gt;{  })
     * </code>
     *
     * @param listener The uncaught exception handler.
     */
    public static void setUncaughtExceptionHandler(IReject listener) {
        globalUncaughtExceptionHandler = listener;
    }

    /**
     * Promise.all({})
     * .then(...)
     *
     * !Warning: All promise object cannot call execute() function.
     *
     * @param list Promise object set.
     * @return Promise
     */
    public static Promise<Object[]> all(List<IPromise> list) {

        if (list == null) {
            throw new RuntimeException("Promise list should not be empty!");
        }

        if (list.size() > 0) {
            Promise<Object[]> p = new Promise<Object[]>(new IExecute<Object[]>() {
                @Override
                public void execute(IResolve<Object[]> resolve, IReject reject) throws Exception {
                    AtomicReference<Exception> ex = new AtomicReference<>(null);
                    AtomicInteger completedCount = new AtomicInteger(0);
                    Object[] result = new Object[list.size()];

                    for (int i = 0; i < list.size(); i++) {
                        if (ex.get() != null) {
                            reject.execute(ex.get());
                            return;
                        }

                        Promise<?> promise;
                        if (list.get(i).isExecutor()) {
                            promise = ((PromiseExecutor<?>)list.get(i)).p;
                        }
                        else {
                            promise = (Promise<?>)list.get(i);
                        }

                        // can call execute
                        Promise<?> ancestor = promise.ancestor == null ? promise : promise.ancestor;
                        if (!ancestor.status.equals(STATUS_PENDING) || ancestor._inExecute == true) {
                            reject.execute(new RuntimeException("Promise is not in pending status"));
                            return;
                        }

                        promise._inTag = i;
                        promise.then(res -> {
                            result[(int) promise._inTag] = res;
                            completedCount.getAndIncrement();
                        }).fail(e->{
                            ex.set(e);
                        }).execute();

                        join(promise);
                    }

                    if (ex.get() != null) {
                        reject.execute(ex.get());
                        return;
                    }
                    if (completedCount.get() == result.length) {
                        resolve.execute(result);
                        return;
                    }
                }
            });
            return p;
        } else {
            try {
                Promise<Object[]> p = new Promise<Object[]>();
                p.resolve(new Object[]{});
                return p;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Promise.all({})
     * .then(...)
     *
     * !Warning: All promise object cannot call execute() function.
     *
     * @param list Promise object set.
     * @return Promise
     */
    public static Promise<Object[]> all(IPromise... list) {

        if (list == null || list.length <= 0) {
            throw new RuntimeException("Promise list should not be empty!");
        }

        return all(Arrays.asList(list));
    }

    /**
     * Wait all promise done. will broke thread.
     *
     * @param list Promise object set.
     */
    public static void join(IPromise... list) {
        join(10, list);
    }

    /**
     * Wait all promise done. will broke thread.
     *
     * @param list Promise object set.
     */
    public static void join(List<IPromise> list) {
        join(10, list);
    }

    /**
     * Wait all promise done. will broke thread.
     *
     * @param list Promise object set.
     * @param peekInMillisecond peek interval.
     */
    public static void join(int peekInMillisecond, IPromise... list) {
        if (list.length == 0) {
            return;
        }

        join(peekInMillisecond, Arrays.asList(list));
    }

    /**
     * Wait all promise done. will broke thread.
     *
     * @param list Promise object set.
     * @param peekInMillisecond peek interval.
     */
    public static void join(int peekInMillisecond, List<IPromise> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        while (true) {
            int doneCount = 0;
            for (int i = 0; i < list.size(); i++) {
                IPromise p1 = list.get(i);
                if (p1.getStatus() != STATUS_PENDING) {
                    doneCount++;
                }
                else if (p1 instanceof PromiseExecutor) {
                    PromiseExecutor<?> pe = (PromiseExecutor<?>)p1;
                    Promise<?> p = pe.p;
                    p = p.ancestor == null? p: p.ancestor;
                    if (!p._inExecute) {
                        p.execute();
                    }
                } else {
                    Promise<?> p = (Promise<?>)p1;
                    p = p.ancestor == null? p: p.ancestor;
                    if (!p._inExecute) {
                        p.execute();
                    }
                }
            }
            if (doneCount == list.size()) {
                return;
            }
            try {
                Thread.sleep(peekInMillisecond);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public boolean isExecutor() { return false; }

    /**
     * Get the current status of promise.
     *
     * @return the status string
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

    private Promise(Promise<?> ancestor) {
        this.ancestor = ancestor;
    }

    /**
     * Construct a promise.
     */
    public Promise() {
        globalObjectSet.add(this);
        this.onExecuteListener = null;
    }

    /**
     * Construct a promise.
     * <code>new Promise((IResolve resolve, IReject reject)-&gt; { resolve.execute(...); });</code>
     *
     * @param listener promise object.
     */
    public Promise(IExecute<TP> listener) {
        globalObjectSet.add(this);
        this.onExecuteListener = listener;
    }

    /**
     * execute the promise.
     *
     * @return Promise interface
     */
    @Override
    public IPromise execute() {
        Promise<?> ancestor = this.ancestor == null ? this : this.ancestor;

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
                                    Promise<?> p = ancestor;
                                    p.status = STATUS_REJECTED;
                                    do {
                                        p._cf = null;
                                        Promise<?> p1 = p.child;
                                        p.child = null;
                                        p = p1;
                                    } while (p != null);
                                }
                            }
                            // 获取最终的结果.
                            else {
                                Promise<?> p = ancestor;
                                if (p.getStatus() == STATUS_PENDING) {
                                    p.status = STATUS_FULFILLED;
                                }
                                do {
                                    p._cf = null;
                                    Promise<?> p1 = p.child;
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

    private IPromise _executeInSync() {
        Promise<?> ancestor = this.ancestor == null ? this : this.ancestor;

        if (!ancestor.status.equals(STATUS_PENDING) || ancestor._inExecute == true) {
            throw new RuntimeException("Promise is not in pending status");
        } else {
            ancestor._inExecute = true;
            Exception ee = null;

            try {
                ancestor.onExecuteListener.execute(res -> {
                    ancestor.resolve(res);
                    return null;
                }, e -> {
                    ancestor.reject(e);
                    return null;
                });
            } catch (Exception e) {
                ee = e;
            } finally {
                globalObjectSet.remove(ancestor);
                if (ee != null) {
                    try {
                        ancestor.reject((Exception) ee);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        // release memory
                        Promise<?> p = ancestor;
                        p.status = STATUS_REJECTED;
                        do {
                            p._cf = null;
                            Promise<?> p1 = p.child;
                            p.child = null;
                            p = p1;
                        } while (p != null);
                    }
                }
                // 获取最终的结果.
                else {
                    Promise<?> p = ancestor;
                    if (p.getStatus() == STATUS_PENDING) {
                        p.status = STATUS_FULFILLED;
                    }
                    do {
                        p._cf = null;
                        Promise<?> p1 = p.child;
                        p.child = null;
                        p = p1;
                    } while (p != null);
                }
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
    public Promise<?> then(IResolve<TP> listener) {
        onSuccessListenerRunnable = null;
        onSuccessListenerNoRet = null;
        onSuccessListener = listener;
        child = new Promise<>(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * After executing asynchronous function the result will be available in the success listener
     * as argument.
     *
     * @param listener IResolve
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise<?> then(IResolveNoRet<TP> listener) {
        onSuccessListenerRunnable = null;
        onSuccessListener = null;
        onSuccessListenerNoRet = listener;
        child = new Promise<>(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * Use runnable to executing asynchronous function. It cannot catch the return value of pre-chain.
     *
     * @param runnable the runnable object
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise<?> then(Runnable runnable) {
        onSuccessListenerRunnable = runnable;
        onSuccessListener = null;
        onSuccessListenerNoRet = null;
        child = new Promise<>(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` chain, any `reject()` occurs in
     * previous async execution this function will be called.
     *
     * @param listener the handler for this fail chain.
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise<?> fail(IReject listener) {
        onErrorListener = listener;
        onErrorListenerNoRet = null;
        child = new Promise<>(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` chain, any `reject()` occurs in
     * previous async execution this function will be called.
     *
     * @param listener the handler for this fail chain.
     * @return It returns a promise for satisfying next chain call.
     */
    public Promise<?> fail(IRejectNoRet listener) {
        onErrorListener = null;
        onErrorListenerNoRet = listener;
        child = new Promise<>(this.ancestor==null?this:this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` or `fail()` chain.
     *
     * @param listener the handler for this finish chain.
     * @return It returns a promise interface. You can call .execute() to activate this promise.
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
     * @throws Exception cause in common scene
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
     * @throws Exception cause in common scene
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
            } else if (onSuccessListenerNoRet != null) {
                onSuccessListenerNoRet.execute((TP) param);
                _handleSuccessAsyncAccept(null);
            } else if (onSuccessListenerRunnable != null) {
                onSuccessListenerRunnable.run();
                _handleSuccessAsyncAccept(null);
            } else if (child != null) {
                child._handleSuccessAsyncRun(param);
            } else if (onFinishListener != null) {
                // Promise ancestor = this.ancestor == null ? this : this.ancestor;
                // globalObjectSet.remove(ancestor);

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
                    Promise<?> p = (Promise<?>) res;
                    Promise<?> pChild = p;
                    while (pChild.child != null) {
                        pChild = pChild.child;
                    }

                    Promise<?> ancestor = p.ancestor == null ? p : p.ancestor;
                    globalObjectSet.remove(ancestor);
                    p.ancestor = this.ancestor;

                    Promise<?> oldP = child;
                    child = p;
                    pChild.child = oldP;
                } else {
                    child = (Promise<?>) res;
                }

                try {
                    child._executeInSync();
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
        if (onErrorListener != null || onErrorListenerNoRet != null) {
            Object res = null;
            try {
                if (onErrorListener != null) {
                    res = onErrorListener.execute(object);
                }
                else {
                    onErrorListenerNoRet.execute(object);
                }
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
                        Promise<?> p = (Promise<?>) res;
                        Promise<?> pChild = p;
                        while (pChild.child != null) {
                            pChild = pChild.child;
                        }

                        Promise<?> oldP = child;
                        child = p;
                        pChild.child = oldP;
                    } else {
                        child = (Promise<?>) res;
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

