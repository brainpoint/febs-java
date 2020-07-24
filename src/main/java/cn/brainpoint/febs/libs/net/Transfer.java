/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-10 01:03
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.brainpoint.febs.libs.net.ssl.AllTrustManager;
import cn.brainpoint.febs.libs.net.ssl.NullHostNameVerifier;

/**
 * The network transfer.
 */
public final class Transfer {

    private static X509TrustManager defaultTrustManager = new AllTrustManager();

    /**
     * set the trust manager.<br>
     * The default trust manager is trust all site.
     * 
     * @param trustManager the trust manager object.
     */
    public static void setDefaultTrustManger(X509TrustManager trustManager) {
        defaultTrustManager = trustManager;
    }

    /**
     * Initial the transfer.
     */
    static {
        HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
    }

    /**
     * Network request
     * 
     * @param param the request parameter
     * @return the network response
     * @throws Exception cause in network io exception or ssl exception.
     */
    @SuppressWarnings("all")
    public static Response request(Request param) throws Exception {
        int readTimeout = param.getTimeout() == 0 ? 5000 : param.getTimeout();
        int connTimeout = readTimeout;
        String method = null == param.getMethod() || param.getMethod().isEmpty() ? "GET" : param.getMethod();
        method = method.toUpperCase();

        Response result;
        PrintWriter out = null;
        try {
            String urlNameString = param.getUrl();

            if (method.equals("GET") && null != param.getBody() && !param.getBody().isEmpty()) {
                urlNameString += "?" + param.getBody();
            }

            URL realUrl = new URL(urlNameString);
            // URL realUrl = new URL(null, urlNameString, new
            // sun.net.www.protocol.https.Handler());

            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;

            // ssl.
            if (urlNameString.indexOf("https://") == 0) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) httpConn;

                /**
                 * get ssl socket factory for connect. e.g.
                 * ((HttpsURLConnection)connect).setSSLSocketFactory(ssf);
                 */
                if (defaultTrustManager != null) {
                    TrustManager[] tm = { defaultTrustManager };
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tm, new java.security.SecureRandom());
                    // 从上述SSLContext对象中得到SSLSocketFactory对象
                    SSLSocketFactory ssf = sslContext.getSocketFactory();

                    httpsConn.setSSLSocketFactory(ssf);
                }
            }

            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            // connection.setRequestProperty("user-agent",
            // "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

            // set headers.
            if (0 < param.getHeaderLength()) {
                for (String key : param.getHeaderKeySet()) {
                    Iterator<String> itr = param.getHeaders(key).iterator();
                    while (itr.hasNext()) {
                        connection.addRequestProperty(key, itr.next());
                    }
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
                if (null != param.getBody()) {
                    out.print(param.getBody());
                }
                // flush输出流的缓冲
                out.flush();
            }

            // 建立实际的连接
            // connection.connect();

            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();

            // remove key == null
            Map<String, List<String>> map1 = new HashMap<>();
            Iterator<Entry<String, List<String>>> itr = map.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<String, List<String>> key = itr.next();
                if (null != key.getKey()) {
                    map1.put(key.getKey(), key.getValue());
                }
            }

            // // 遍历所有的响应头字段
            // for (String key : map.keySet()) {
            // System.out.println(key + "--->" + map.get(key));
            // }
            result = new Response(connection, map1);
            result.setStatusCode(((HttpURLConnection) connection).getResponseCode());
            result.setStatusMsg(((HttpURLConnection) connection).getResponseMessage());
        } catch (Exception e) {
            // System.out.println("发送请求出现异常！" + e);
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