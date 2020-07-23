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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.brainpoint.febs.exception.FebsException;
import cn.brainpoint.febs.exception.FebsRuntimeException;
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
 * <i>e.g.</i> <code>
 *  PromiseFuture future = new Promise((resolve, reject)-&gt;{  resolve.execute();  })
 *    .then(()-&gt;{})
 *    .then(()-&gt;{})
 *    .fail((e)-&gt;{})
 *    .execute();
 *
 *  future.get();
 * </code>
 *
 * @author pengxiang.li
 */
public class Promise<T> implements java.lang.Comparable<T>, IPromise {

    static {
        Febs.init();
    }

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_FULFILLED = "fulfilled";
    public static final String STATUS_REJECTED = "rejected";
    private static ConcurrentSkipListSet<Object> globalObjectSet = new ConcurrentSkipListSet<>();
    private static IReject globalUncaughtExceptionHandler;

    private Runnable onSuccessListenerRunnable;
    private IResolve<T> onSuccessListener;
    private IResolveNoRet<T> onSuccessListenerNoRet;
    private IReject onErrorListener;
    private IRejectNoRet onErrorListenerNoRet;

    private IFinish onFinishListener;
    private IExecute<T> onExecuteListener;
    private Promise<?> child;
    private String status = STATUS_PENDING;
    private Object tag;
    private Promise<?> ancestor;
    private boolean inExecute = false;
    private Object inTag; // internal use.
    private Object inResult;
    private Object inResultTmp; // 结果先存储至临时位置.
    private CompletableFuture<?> inCf;
    private Lock inLock;
    private Condition inCondition;

    private static class PromiseExecutor<T> implements IPromise {
        private Promise<T> p;

        public PromiseExecutor(Promise<T> p) {
            this.p = p;
        }

        @Override
        public boolean isExecutor() {
            return true;
        }

        @Override
        public PromiseFuture execute() {
            return this.p.execute();
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

    public static String dumpDebug() {
        return "globalObjectSet: " + globalObjectSet.size();
    }

    /**
     * Some promise object will catch exception use this method, if it have't call
     * .fail() <i>e.g.</i> <code>
     *    Promise.setUncaughtExceptionHandler((e)-&gt;{  })
     * </code>
     *
     * @param listener The uncaught exception handler.
     */
    public static void setUncaughtExceptionHandler(IReject listener) {
        globalUncaughtExceptionHandler = listener;
    }

    /**
     * Promise.all({}) .then(...)
     *
     * !Warning: All promise object cannot call execute() function.
     *
     * @param list Promise object set.
     * @return Promise
     */
    public static Promise<Object[]> all(List<IPromise> list) {

        if (list == null) {
            throw new FebsRuntimeException("Promise list should not be empty!");
        }

        if (!list.isEmpty()) {
            return new Promise<>(new IExecute<Object[]>() {
                @Override
                public void execute(IResolve<Object[]> resolve, IReject reject) throws Exception {
                    AtomicReference<Exception> ex = new AtomicReference<>(null);
                    // AtomicInteger completedCount = new AtomicInteger(0);
                    Object[] result = new Object[list.size()];

                    for (int i = 0; i < list.size(); i++) {
                        if (ex.get() != null) {
                            reject.execute(ex.get());
                            return;
                        }

                        Promise<?> promise;
                        if (list.get(i).isExecutor()) {
                            promise = ((PromiseExecutor<?>) list.get(i)).p;
                        } else {
                            promise = (Promise<?>) list.get(i);
                        }

                        // can call execute
                        Promise<?> ancestor = promise.ancestor == null ? promise : promise.ancestor;
                        if (!ancestor.status.equals(STATUS_PENDING) || ancestor.inExecute) {
                            reject.execute(new FebsRuntimeException("Promise is not in pending status"));
                            return;
                        }

                        try {
                            promise.inTag = i;
                            promise.then(res -> {
                                result[(int) promise.inTag] = res;
                                // completedCount.getAndIncrement();
                            }).fail(e -> {
                                ex.set(e);
                            }).execute().get();
                        } catch (ExecutionException e) {
                            ex.set(e);
                            break;
                        }
                    } // for.

                    if (ex.get() != null) {
                        reject.execute(ex.get());
                        return;
                    }

                    // if (completedCount.get() == result.length) {
                    resolve.execute(result);
                    // }
                }
            });
        } else {
            try {
                Promise<Object[]> p = new Promise<>();
                p.resolve(new Object[] {});
                return p;
            } catch (Exception e) {
                throw new FebsRuntimeException(e);
            }
        }
    }

    /**
     * Promise.all({}) .then(...)
     *
     * !Warning: All promise object cannot call execute() function.
     *
     * @param list Promise object set.
     * @return Promise
     */
    public static Promise<Object[]> all(IPromise... list) {

        if (list == null || list.length <= 0) {
            throw new FebsRuntimeException("Promise list should not be empty!");
        }

        return all(Arrays.asList(list));
    }

    @Override
    public boolean isExecutor() {
        return false;
    }

    /**
     * Get the current status of promise.
     *
     * @return the status string
     */
    @Override
    public String getStatus() {
        return this.ancestor == null ? this.status : this.ancestor.status;
    }

    public Object getResult() {
        if (!this.getStatus().equals(STATUS_FULFILLED)) {
            return null;
        }

        getLock().lock();
        Object ret = this.ancestor == null ? this.inResult : this.ancestor.inResult;
        getLock().unlock();
        return ret;
    }

    private void setResult() {
        getLock().lock();
        if (this.ancestor == null) {
            if (this.inResult == null) {
                this.inResult = this.inResultTmp;
                this.inResultTmp = null;
            }
        } else {
            if (this.ancestor.inResult == null) {
                this.ancestor.inResult = this.ancestor.inResultTmp;
                this.ancestor.inResultTmp = null;
            }
        }
        getLock().unlock();
    }

    private void setResultTmp(Object obj) {
        getLock().lock();
        if (this.ancestor == null) {
            this.inResultTmp = obj;
        } else {
            this.ancestor.inResultTmp = obj;
        }
        getLock().unlock();
    }

    private Lock getLock() {
        if (this.ancestor == null) {
            if (inLock == null) {
                inLock = new ReentrantLock();
            }
            return inLock;
        } else {
            if (this.ancestor.inLock == null) {
                this.ancestor.inLock = new ReentrantLock();
            }
            return this.ancestor.inLock;
        }
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
    public Promise(IExecute<T> listener) {
        globalObjectSet.add(this);
        this.onExecuteListener = listener;
    }

    /**
     * execute the promise.
     *
     * @return Promise interface
     */
    @Override
    public PromiseFuture execute() {
        Promise<?> ancestor = this.ancestor == null ? this : this.ancestor;

        if (!ancestor.status.equals(STATUS_PENDING) || ancestor.inExecute) {
            throw new FebsRuntimeException("Promise is not in pending status");
        } else {
            ancestor.inExecute = true;

            if (ancestor.inLock != null || ancestor.inCondition != null) {
                throw new FebsRuntimeException("Promise lock error");
            }
            // ancestor.inLock = new ReentrantLock();
            ancestor.inCondition = getLock().newCondition();

            try {
                ancestor.inCf = CompletableFuture.supplyAsync(() -> {
                    try {
                        ancestor.onExecuteListener.execute(res -> {

                            setResultTmp(res);
                            ancestor.resolve(res);

                            return null;
                        }, e -> {
                            setResultTmp(e);
                            ancestor.reject(e);
                            // setResultTmp(e); don't do it

                            return null;
                        });
                    } catch (Exception e) {
                        setResultTmp(e);

                        throw new FebsRuntimeException(e);
                    }
                    return null;
                }, Febs.getExecutorService()).handle((res, e) -> {
                    globalObjectSet.remove(ancestor);
                    if (e != null) {
                        try {
                            setResultTmp(e);
                            ancestor.reject((Exception) e);
                            // setResultTmp(e); don't do it
                        } catch (Exception ex) {
                            setResultTmp(ex);
                            throw new FebsRuntimeException(ex);
                        } finally {
                            // release memory
                            Promise<?> p = ancestor;
                            p.status = STATUS_REJECTED;
                            do {
                                p.inCf = null;
                                Promise<?> p1 = p.child;
                                p.child = null;
                                p = p1;
                            } while (p != null);

                            setResult();
                            ancestor.inLock.lock();
                            ancestor.inCondition.signalAll();
                            ancestor.inLock.unlock();
                        }
                    }
                    // 获取最终的结果.
                    else {
                        Promise<?> p = ancestor;
                        if (STATUS_PENDING.equals(p.getStatus())) {
                            p.status = STATUS_FULFILLED;
                        }

                        setResult();
                        do {
                            p.inCf = null;
                            Promise<?> p1 = p.child;
                            p.child = null;
                            p = p1;
                        } while (p != null);

                        ancestor.inLock.lock();
                        ancestor.inCondition.signalAll();
                        ancestor.inLock.unlock();
                    }
                    return null;
                });
            } catch (Exception e) {
                throw new FebsRuntimeException(e);
            }
        }

        return new PromiseFuture(this, ancestor.inLock, ancestor.inCondition);
    }

    private IPromise executeInSync() {
        Promise<?> ancestor = this.ancestor == null ? this : this.ancestor;

        if (!ancestor.status.equals(STATUS_PENDING) || ancestor.inExecute) {
            throw new FebsRuntimeException("Promise is not in pending status");
        } else {
            ancestor.inExecute = true;
            Exception ee = null;

            try {
                ancestor.resolve(null);
            } catch (Exception e) {
                ee = e;
            }

            globalObjectSet.remove(ancestor);
            if (ee != null) {
                try {
                    ancestor.reject(ee);
                } catch (Exception ex) {
                    throw new FebsRuntimeException(ex);
                } finally {
                    // release memory
                    Promise<?> p = ancestor;
                    p.status = STATUS_REJECTED;
                    do {
                        p.inCf = null;
                        Promise<?> p1 = p.child;
                        p.child = null;
                        p = p1;
                    } while (p != null);
                }
            }
            // 获取最终的结果.
            else {
                Promise<?> p = ancestor;
                if (STATUS_PENDING.equals(p.getStatus())) {
                    p.status = STATUS_FULFILLED;
                }
                do {
                    p.inCf = null;
                    Promise<?> p1 = p.child;
                    p.child = null;
                    p = p1;
                } while (p != null);
            }
        }

        return this;
    }

    /**
     * After executing asynchronous function the result will be available in the
     * success listener as argument.
     *
     * @param listener IResolve
     * @return It returns a promise for satisfying next chain call.
     */
    @SuppressWarnings("all")
    public Promise<?> then(IResolve<T> listener) {
        onSuccessListenerRunnable = null;
        onSuccessListenerNoRet = null;
        onSuccessListener = listener;
        child = new Promise<>(this.ancestor == null ? this : this.ancestor);
        return child;
    }

    /**
     * After executing asynchronous function the result will be available in the
     * success listener as argument.
     *
     * @param listener IResolve
     * @return It returns a promise for satisfying next chain call.
     */
    @SuppressWarnings("all")
    public Promise<?> then(IResolveNoRet<T> listener) {
        onSuccessListenerRunnable = null;
        onSuccessListener = null;
        onSuccessListenerNoRet = listener;
        child = new Promise<>(this.ancestor == null ? this : this.ancestor);
        return child;
    }

    /**
     * Use runnable to executing asynchronous function. It cannot catch the return
     * value of pre-chain.
     *
     * @param runnable the runnable object
     * @return It returns a promise for satisfying next chain call.
     */
    @SuppressWarnings("all")
    public Promise<?> then(Runnable runnable) {
        onSuccessListenerRunnable = runnable;
        onSuccessListener = null;
        onSuccessListenerNoRet = null;
        child = new Promise<>(this.ancestor == null ? this : this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` chain, any `reject()`
     * occurs in previous async execution this function will be called.
     *
     * @param listener the handler for this fail chain.
     * @return It returns a promise for satisfying next chain call.
     */
    @SuppressWarnings("all")
    public Promise<?> fail(IReject listener) {
        onErrorListener = listener;
        onErrorListenerNoRet = null;
        child = new Promise<>(this.ancestor == null ? this : this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` chain, any `reject()`
     * occurs in previous async execution this function will be called.
     *
     * @param listener the handler for this fail chain.
     * @return It returns a promise for satisfying next chain call.
     */
    @SuppressWarnings("all")
    public Promise<?> fail(IRejectNoRet listener) {
        onErrorListener = null;
        onErrorListenerNoRet = listener;
        child = new Promise<>(this.ancestor == null ? this : this.ancestor);
        return child;
    }

    /**
     * This function must call at the end of the `then()` or `fail()` chain.
     *
     * @param listener the handler for this finish chain.
     * @return It returns a promise interface. You can call .execute() to activate
     *         this promise.
     */
    public IPromise finish(IFinish listener) {
        onFinishListener = listener;
        return new PromiseExecutor<T>(this);
    }

    /**
     * Call this function with your resultant value, it will be available in
     * following `then()` function call.
     *
     * @param object your resultant value (any type of data you can pass as argument
     *               e.g. int, String, List, Map, any Java object)
     * @return This will return the resultant value you passed in the function call
     * @throws Exception cause in common scene
     */
    private void resolve(Object object) {
        // status = STATUS_FULFILLED;
        handleSuccess(object);
    }

    /**
     * Call this function with your error value, it will be available in following
     * `fail()` function call.
     *
     * @param object your error value (any type of data you can pass as argument
     *               e.g. int, String, List, Map, any Java object)
     * @throws Exception cause in common scene
     */
    private void reject(Exception object) throws Exception {
        // status = STATUS_REJECTED;
        handleError(object);
    }

    private Object handleSuccessAsyncRun(Object param) {

        try {
            if (onSuccessListener != null) {
                Object res = onSuccessListener.execute((T) param);
                this.setResultTmp(res);
                handleSuccessAsyncAccept(res);
            } else if (onSuccessListenerNoRet != null) {
                onSuccessListenerNoRet.execute((T) param);
                this.setResultTmp(null);
                handleSuccessAsyncAccept(null);
            } else if (onSuccessListenerRunnable != null) {
                onSuccessListenerRunnable.run();
                this.setResultTmp(null);
                handleSuccessAsyncAccept(null);
            } else if (child != null) {
                child.handleSuccessAsyncRun(param);
            } else if (onFinishListener != null) {
                // Promise ancestor = this.ancestor == null ? this : this.ancestor;
                // globalObjectSet.remove(ancestor);
                onFinishListener.execute();
            }
            return null;
        } catch (Exception e) {
            this.setResultTmp(e);
            throw new FebsRuntimeException(e);
        }
    }

    private void handleSuccessAsyncAccept(Object res) {

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
                    child.executeInSync();
                } catch (Exception e) {
                    throw new FebsRuntimeException(e);
                }
            } else if (child != null) {
                try {
                    child.resolve(res);
                } catch (Exception e) {
                    throw new FebsRuntimeException(e);
                }
            } else if (onFinishListener != null) {
                onFinishListener.execute();
            }
        } else {
            if (child != null) {
                try {
                    child.resolve(res);
                } catch (Exception e) {
                    throw new FebsRuntimeException(e);
                }
            } else if (onFinishListener != null) {
                onFinishListener.execute();
            }
        }
    }

    private void handleSuccess(Object object) {
        // 在异步线程中, 不重新启动新线程.
        this.handleSuccessAsyncRun(object);
    }

    private void handleError(java.lang.Exception object) throws Exception {
        setResultTmp(object);
        if (onErrorListener != null || onErrorListenerNoRet != null) {
            Object res = null;
            try {
                if (onErrorListener != null) {
                    res = onErrorListener.execute(object);
                } else {
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
            FebsException ex = new FebsException("Promise uncaught exception", object);
            if (globalUncaughtExceptionHandler != null) {
                globalUncaughtExceptionHandler.execute(ex);
            }
            setResultTmp(ex);
            onFinishListener.execute();
        }
    }
}
