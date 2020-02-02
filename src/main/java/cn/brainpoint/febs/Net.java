/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import cn.brainpoint.febs.libs.net.Request;
import cn.brainpoint.febs.libs.net.Response;
import cn.brainpoint.febs.libs.net.Transfer;
import cn.brainpoint.febs.libs.promise.IReject;
import cn.brainpoint.febs.libs.promise.IResolve;

/**
 * @Author pengxiang.li
 * @Date 2020/1/30 8:55 下午
 */
public class Net {

    /**
     * 进行网络请求. 使用promise方式的异步操作.
     *
     * @param request
     * @return
     */
    public static Promise<Response> fetch(Request request) {
        return new Promise<Response>(
                (IResolve<Response> resolve, IReject reject)->{
                    try {
                        Response resp = Transfer.request(request);
                        resolve.execute(resp);
                    } catch (Exception e) {
                        reject.execute(e);
                    }
                });
    }
}
