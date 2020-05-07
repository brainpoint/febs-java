/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 响应结果
 *
 * @Author pengxiang.li
 * @Date 2020/1/31 7:24 下午
 */
public class Response {

    private String _content;
    private URLConnection _connection;

    /**
     * The headers of response.
     */
    public Map<String, List<String>> headers;

    /**
     * The status code of response.
     */
    public int statusCode;

    /**
     * The status message of response.
     */
    public String statusMsg;

    public Response(URLConnection connection) {
        this._content = null;
        this._connection = connection;
    }

    /**
     * Get the string content.
     * @return string content
     */
    public String text() throws IOException {
        if (this._content != null) {
            return this._content;
        }

        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = null;
        this._content = "";

        try {
            in = new BufferedReader(new InputStreamReader(this._connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                this._content += line;
            }
        }
        catch (Exception e) {
            this._content = null;
            throw e;
        }
        finally {
            if (in != null) {
                in.close();
            }
        }

        return this._content;
    }

    /**
     * Get the binary content.
     *
     * Warning: Must call bufferedReader.close() after read buffer.
     *
     * @return binary content
     */
    public BufferedReader blob() throws IOException {

        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(this._connection.getInputStream()));
        }
        catch (Exception e) {
            if (in != null) {
                in.close();
            }
            throw e;
        }

        return in;
    }
}