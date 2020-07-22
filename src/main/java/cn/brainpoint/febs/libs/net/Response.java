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
import java.util.List;
import java.util.Map;

/**
 * The reponse result of network transfer.
 *
 * @author pengxiang.li
 */
public class Response extends Headers {

    private String content;
    private URLConnection connection;
    private int statusCode;
    private String statusMsg;

    /**
     * The status code of response.
     * 
     * @return status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * The status code of response.
     * 
     * @param v status code
     */
    public void setStatusCode(int v) {
        statusCode = v;
    }

    /**
     * The status message of response.
     * 
     * @return status message
     */
    public String getStatusMsg() {
        return statusMsg;
    }

    /**
     * The status message of response.
     * 
     * @param v status message
     */
    public void setStatusMsg(String v) {
        statusMsg = v;
    }

    public Response(URLConnection connection) {
        super();
        this.content = null;
        this.connection = connection;
    }

    public Response(URLConnection connection, Map<String, List<String>> headerSet) {
        super(headerSet);
        this.content = null;
        this.connection = connection;
    }

    /**
     * Get the string content.
     * 
     * @return string content
     * @throws IOException cause in network io error.
     */
    public String text() throws IOException {
        if (this.content != null) {
            return this.content;
        }

        // 定义 BufferedReader输入流来读取URL的响应
        this.content = null;
        StringBuilder con = new StringBuilder("");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(this.connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                con.append(line);
            }
        }

        this.content = con.toString();
        return this.content;
    }

    /**
     * Get the binary content.
     *
     * Warning: Must call bufferedReader.close() after read buffer.
     *
     * @return binary content
     * @throws IOException cause in network io error.
     */
    public BufferedReader blob() throws IOException {

        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
        return in;
    }
}