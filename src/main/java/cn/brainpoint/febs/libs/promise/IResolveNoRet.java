/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @author pengxiang.li
 */
public interface IResolveNoRet<T> {
    void execute(T object) throws Exception;
}
