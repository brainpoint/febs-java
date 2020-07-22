/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @author pengxiang.li
 */
public interface IPromise {
    /**
     * execute the promise.
     *
     * @return promise interface.
     */
    IPromise execute();

    /**
     * The current status of Promise.
     *
     * @return the string of status
     */
    String getStatus();

    Object getTag();

    void setTag(Object tag);

    boolean isExecutor();
}
