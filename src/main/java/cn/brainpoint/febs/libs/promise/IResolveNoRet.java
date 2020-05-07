/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @Author pengxiang.li
 * @Date 2020/1/31 7:19 下午
 */
public interface IResolveNoRet<T> {
    void execute(T object) throws Exception;
}
