/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import org.junit.Test;

import cn.brainpoint.febs.libs.promise.IReject;
import cn.brainpoint.febs.libs.promise.IResolve;

/**
 * @author pengxiang.li
 */
public class PromiseTest {

    // resolve.
    public Promise makePromiseTimeout() {
        Promise promise = new Promise((IResolve resolve, IReject reject) -> {
            Log.out("begin %d %d", System.currentTimeMillis(), Thread.currentThread().getId());
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            resolve.execute(null);
        });
        return promise;
    }

    public Promise makePromiseReject() {
        Promise promise = new Promise((IResolve resolve, IReject reject) -> {
            reject.execute(null);
        });
        return promise;
    }

    public Promise<Integer> makePromiseTemplate2() {
        Promise<Integer> promise = new Promise<Integer>((IResolve<Integer> resolve, IReject reject) -> {
            resolve.execute(2);
        });
        return promise;
    }

    public Promise makePromiseException() {
        Promise promise = new Promise((IResolve resolve, IReject reject) -> {
            throw new Exception("break in promise");
        });
        return promise;
    }

    /**
     * 测试 then的可靠性.
     */
    @Test
    public void testTimeoutAndThen() {
        String tag = "promise TimeoutAndThen: ";
        Log.out("========================================");
        Log.out(tag + "begin");
        Promise promise = makePromiseTimeout();
        long now = System.currentTimeMillis();

        promise.then(res -> {
            return Febs.Utils.sleep(1000);
        }).then(res -> {
            Log.out(tag + "then");
            long ml = Math.abs(System.currentTimeMillis() - now - 2000);
            Log.out(tag + " ms diff: %d", ml);
            if (ml > 20) {
                Log.err(tag + " 1");
            } else {
                Log.out(tag + " 1");
            }
            return 1;
        }).then(res -> {
            Log.out(tag + "then");
            if ((int) res == 1) {
                Log.out(tag + " 2");
            } else {
                Log.err(tag + " 2");
            }
            return 2;
        }).then(res -> {
            Log.out(tag + "then");
            if ((int) res == 2) {
                Log.out(tag + " 3");
            } else {
                Log.err(tag + " 3");
            }
            promise.setTag(3);
            return null;
        }).finish(() -> {
            if ((int) promise.getTag() != 3) {
                Log.err(tag + "finish");
            } else {
                Log.out(tag + "finish");
            }
            Log.out(tag + "dump: " + Promise.dumpDebug());
        }).execute();

        Promise.join(promise);
    }

    /**
     * 测试fail是否能正确触发.
     */
    @Test
    public void testFail() {
        String tag = "promise Fail: ";
        Log.out("========================================");
        Log.out(tag + "begin");
        Promise promise = makePromiseReject();

        promise.then(res -> {
            Log.out(tag + "then");
            Log.err(tag + " 1");
            return null;
        }).fail(e -> {
            Log.out(tag + "fail");
            Log.out(tag + " 1");
            return 2;
        }).fail(e -> {
            Log.out(tag + "fail");
            Log.out(tag + " 11");
            return 2;
        }).then(res -> {
            Log.out(tag + "then");
            if ((int) res != 2) {
                Log.err(tag + " 2");
            } else {
                Log.out(tag + " 2");
            }
            promise.setTag(2);
            return null;
        }).fail(e -> {
            Log.out(e.getLocalizedMessage());
            Log.out(tag + "fail");
            Log.out(tag + " 12");
            return 2;
        }).finish(() -> {
            if ((int) promise.getTag() == 2) {
                Log.out(tag + "finish");
            } else {
                Log.err(tag + "finish");
            }
            Log.out(tag + "dump: " + Promise.dumpDebug());
            promise.setTag(3);
        }).execute();

        Promise.join(promise);
        if ((int) promise.getTag() == 3) {
            Log.out(tag + "finish 2");
        } else {
            Log.err(tag + "finish 2");
        }
    }

    /**
     * 测试模板类型.
     */
    @Test
    public void testTemplate() {
        String tag = "promise Template: ";
        Log.out("========================================");
        Log.out(tag + "begin");
        Promise<Integer> promise = makePromiseTemplate2();

        promise.then((Integer res) -> {
            Log.out(tag + "then");
            if (res == 2) {
                Log.out(tag + " 1");
            } else {
                Log.err(tag + " 1");
            }
            return 2;
        }).fail((Exception e) -> {
            Log.out(tag + "fail");
            Log.out(e.getLocalizedMessage());
            Log.err(tag + " 1");
            return 2;
        }).then(res -> {
            Log.out(tag + "then");
            if ((Integer) res != 2) {
                Log.err(tag + " 2");
            } else {
                Log.out(tag + " 2");
            }
        }).finish(() -> {
            Log.out(tag + "finish 2");
            Log.out(tag + "dump: " + Promise.dumpDebug());
        }).execute();

        Promise.join(promise);
    }

    /**
     * 测试异常情况.
     */
    @Test
    public void testException() {
        // Febs.init(new Febs.ThreadPoolCfg(2, 4, 20000, new ArrayBlockingQueue<>(20),
        // new ThreadPoolExecutor.AbortPolicy()));
        String tag = "promise Exception: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        Promise.setUncaughtExceptionHandler(e -> {
            Log.out(e.getLocalizedMessage());
            return null;
        });

        Promise promise = makePromiseException();

        promise.then(res -> {
            Log.err(tag + "then");
            return 2;
        }).fail((Exception e) -> {
            Log.out(tag + "fail");
            Log.out(e.getLocalizedMessage());
            promise.setTag(1);
            return 2;
        }).finish(() -> {
            Log.out(tag + "finish 2");
        }).execute();

        Promise.join(promise);
        if ((int) promise.getTag() == 1) {
            Log.out(tag + "finish 3");
        } else {
            Log.err(tag + "finish 3");
        }

        Promise promise2 = makePromiseException();
        try {
            promise2.then(res -> {
                Log.err(tag + "then");
                return 2;
            }).finish(() -> {
                Log.out(tag + "finish 4");
            }).execute();
            Promise.join(promise2);
        } catch (Exception e) {
            Log.out(tag + e.getLocalizedMessage());
            promise2.setTag(1);
        }
    }
}
