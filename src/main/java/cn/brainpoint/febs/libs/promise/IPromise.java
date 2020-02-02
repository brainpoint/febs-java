/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @Author pengxiang.li
 * @Date 2020/2/1 9:15 下午
 */
public interface IPromise {
    /**
     * execute the promise.
     */
    IPromise execute();

    /**
     * 当前的状态.
     *
     * @return
     */
    String getStatus();

    Object getTag();
    void setTag(Object tag);
}
