/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-10 01:03
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

import cn.brainpoint.febs.libs.net.ssl.AllTrustManager;
import cn.brainpoint.febs.libs.net.ssl.NullHostNameVerifier;

import javax.net.ssl.HttpsURLConnection;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * 网络请求工具
 */
public final class Transfer {

    private static boolean inited = false;

    /**
     * inital the transfer.
     */
    private static void init() {
        if (!Transfer.inited) {
            Transfer.inited = true;
            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
        }
    }

    /**
     * 向指定URL发送请求
     *
     * @return 所代表远程资源的响应结果
     */
    public static Response request(Request param)
    throws Exception
    {
        int readTimeout = param.timeout == 0 ? 5000: param.timeout;
        int connTimeout = readTimeout;
        String method = null == param.method || param.method.length() == 0 ? "GET": param.method;
        method = method.toUpperCase();

        init();

        Response result;
        PrintWriter out = null;
        try {
            String urlNameString = param.url;

            if (method.equals("GET")) {
                if (null != param.body) {
                    urlNameString += "?" + param.body;
                }
            }

            URL realUrl = new URL(urlNameString);
//            URL realUrl = new URL(null, urlNameString, new sun.net.www.protocol.https.Handler());

            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;

            result = new Response(connection);

            // ssl.
            if (urlNameString.indexOf("https://") == 0) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) httpConn;
                httpsConn.setSSLSocketFactory(AllTrustManager.getSocketFactory());
            }

            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
//            connection.setRequestProperty("user-agent",
//                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            // set headers.
            if (null != param.headers) {
                for (String key : param.headers.keySet()) {
                    connection.setRequestProperty(key, param.headers.get(key));
                }
            }


            // 发送POST请求必须设置如下两行
            if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setDoInput(true);
            }

            connection.setConnectTimeout(connTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setUseCaches(false);
            httpConn.setRequestMethod(method);

            // 获取URLConnection对象对应的输出流
            if (method.equals("POST")) {
                out = new PrintWriter(connection.getOutputStream());
                // 发送请求参数
                if (null != param.body) {
                    out.print(param.body);
                }
                // flush输出流的缓冲
                out.flush();
            }

            // 建立实际的连接
//            connection.connect();

            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
            result.headers = map;
            result.statusCode = ((HttpURLConnection) connection).getResponseCode();
            result.statusMsg = ((HttpURLConnection) connection).getResponseMessage();
        }
        catch (Exception e) {
//            System.out.println("发送请求出现异常！" + e);
            e.printStackTrace();
            throw e;
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }
}