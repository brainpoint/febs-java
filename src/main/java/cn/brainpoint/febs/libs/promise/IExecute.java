/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @author pengxiang.li
 */
public interface IExecute<T> {
    void execute(IResolve<T> resolve, IReject reject) throws Exception;
}