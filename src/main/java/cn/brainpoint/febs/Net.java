/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import javax.net.ssl.X509TrustManager;

import cn.brainpoint.febs.libs.net.Request;
import cn.brainpoint.febs.libs.net.Response;
import cn.brainpoint.febs.libs.net.Transfer;
import cn.brainpoint.febs.libs.promise.IReject;
import cn.brainpoint.febs.libs.promise.IResolve;

/**
 * The network utility.
 *
 * @author pengxiang.li
 */
public class Net {

    static {
        Febs.init();
    }

    public Net() {
    }

    /**
     * set the trust manager.<br>
     * The default trust manager is trust all site.
     * 
     * @param trustManager the trust manager object.
     */
    public static void setDefaultTrustManger(X509TrustManager trustManager) {
        Transfer.setDefaultTrustManger(trustManager);
    }

    /***
     * The network transfer in fetch style.
     *
     * @param url request url.
     * @return Promise object
     */
    public Promise<Response> fetch(String url) {
        Request req = new Request(url, null, "get");
        return fetch(req);
    }

    /**
     * The network transfer in fetch style.
     *
     * @param request request parameter.
     * @return Promise object
     */
    public Promise<Response> fetch(Request request) {
        return new Promise<>((IResolve<Response> resolve, IReject reject) -> {
            try {
                Response resp = Transfer.request(request);
                resolve.execute(resp);
            } catch (Exception e) {
                reject.execute(e);
            }
        });
    }
}
