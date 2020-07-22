/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.brainpoint.febs.exception.FebsRuntimeException;

/**
 * @author pengxiang.li
 */
public class Febs {

    static {
        init();
    }

    private static boolean inited = false;
    private static ExecutorService executorService = null;

    /**
     * The network transfer in fetch style.
     */
    public static final Net Net = new Net();

    /**
     * The utilities: Timer and so on.
     */
    public static final Utils Utils = new Utils();

    /**
     * This thread pool config will use in Promise.
     */
    public static class ThreadPoolCfg {
        public int corePoolSize = 2;
        public int maximumPoolSize = 4;
        /**
         * in millisecond.
         */
        public int keepAliveTime = 20000;
        public BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        public RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

        public ThreadPoolCfg() {
        }

        public ThreadPoolCfg(Integer corePoolSize, Integer maximumPoolSize, Integer keepAliveTime,
                BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            if (maximumPoolSize != null) {
                this.maximumPoolSize = maximumPoolSize.intValue();
            }
            if (keepAliveTime != null) {
                this.keepAliveTime = keepAliveTime.intValue();
            }
            if (corePoolSize != null) {
                this.corePoolSize = corePoolSize.intValue();
            }
            if (workQueue != null) {
                this.workQueue = workQueue;
            }
            if (handler != null) {
                this.handler = handler;
            }
        }
    }

    /**
     * Get a executor service. <i>e.g.</i> <code>
         try {
            Future&lt;Object&gt; future = Febs.getExecutorService.submit(()-&gt;{
                                        // do anything in this thread...
                                        return "any";
                                    });
             Object result = future.get();
         } catch (ExecutionException e) {
             e.printStackTrace();
         } catch (InterruptedException e) {
             e.printStackTrace();
         } catch (Exception e) {
             e.printStackTrace();
         }
     * </code>
     *
     * @return get executorService.
     */
    public static ExecutorService getExecutorService() {
        if (executorService == null) {
            throw new FebsRuntimeException("Library uninitialized run \"Febs.init()\" first");
        }
        return executorService;
    }

    /**
     * Initial
     */
    public static void init() {
        if (inited) {
            return;
        }
        inited = true;
        init(new ThreadPoolCfg());
    }

    /**
     * Initial with thread pool config.
     * 
     * @param threadPoolCfg thread pool config.
     */
    public static void init(ThreadPoolCfg threadPoolCfg) {
        _initThreadPool(threadPoolCfg);
    }

    private static void _initThreadPool(ThreadPoolCfg threadPoolCfg) {
        if (null != executorService) {
            executorService.shutdownNow();
            executorService = null;
        }
        executorService = new ThreadPoolExecutor(threadPoolCfg.corePoolSize, threadPoolCfg.maximumPoolSize,
                threadPoolCfg.keepAliveTime, TimeUnit.MILLISECONDS, threadPoolCfg.workQueue, threadPoolCfg.handler);
    }
}
