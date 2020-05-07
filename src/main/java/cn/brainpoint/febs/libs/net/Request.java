/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

import java.util.HashMap;

/**
 * 请求参数
 *
 * @Author pengxiang.li
 * @Date 2020/1/31 7:22 下午
 */
public class Request {
    public String url;
    public String body;
    public String method;
    public HashMap<String, String> headers;

    /**
     * 默认值 (5000)
     */
    public int timeout = 5000;

    public Request() {
    }

    public Request(String url) {
        this.url = url;
        this.body = null;
        this.method = null;
        this.headers = null;
    }
    public Request(String url, String body, String method, HashMap<String, String> headers) {
        this.url = url;
        this.body = body;
        this.method = method;
        this.headers = headers;
    }
    public Request(String url, String body, String method, HashMap<String, String> headers, int timeout) {
        this.timeout = timeout;
        this.url = url;
        this.body = body;
        this.method = method;
        this.headers = headers;
    }
}
