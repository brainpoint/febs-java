/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.promise;

/**
 * @Author pengxiang.li
 * @Date 2020/1/31 7:20 下午
 */
public interface IRejectNoRet {
    void execute(Exception object) throws Exception;
}