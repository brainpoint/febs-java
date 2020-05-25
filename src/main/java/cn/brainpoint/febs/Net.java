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
 * The network utility.
 *
 * @author pengxiang.li
 * <b>date</b> 2020/1/30 8:55 下午
 */
public class Net {

    static {
        Febs.init();
    }

    /***
     * The network transfer in fetch style.
     *
     * @param url request url.
     * @return Promise object
     */
    public static Promise<Response> fetch(String url) {
        return Net.fetch(new Request(url, null, "get", null));
    }

    /**
     * The network transfer in fetch style.
     *
     * @param request request parameter.
     * @return Promise object
     */
    public static Promise<Response> fetch(Request request) {
        return new Promise<>(
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
