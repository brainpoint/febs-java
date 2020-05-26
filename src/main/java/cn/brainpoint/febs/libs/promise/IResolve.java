/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @author pengxiang.li
 * @date  2020/1/31 7:19 下午
 */
public interface IResolve<T> {
    Object execute(T object) throws Exception;
}
