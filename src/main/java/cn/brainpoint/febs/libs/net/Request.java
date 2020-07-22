/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

/**
 * The request parameter of network transfer.
 *
 * @author pengxiang.li
 */
public class Request extends Headers {
    private String url;
    private String body;
    private String method;
    private int timeout = 5000;

    public String getUrl() {
        return url;
    }

    public void setUrl(String v) {
        url = v;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String v) {
        body = v;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String v) {
        method = v;
    }

    /**
     * Connect and receive timeout. default in 5000ms.
     * 
     * @return timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * set Connect and receive timeout.
     * 
     * @param v timeout
     */
    public void setTimeout(int v) {
        timeout = v;
    }

    public Request() {
        super();
    }

    public Request(String url) {
        super();
        this.url = url;
        this.body = null;
        this.method = null;
    }

    public Request(String url, String body, String method) {
        super();
        this.url = url;
        this.body = body;
        this.method = method;
    }

    public Request(String url, String body, String method, int timeout) {
        super();
        this.timeout = timeout;
        this.url = url;
        this.body = body;
        this.method = method;
    }
}
