/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-17 17:13
 * Desc:
 * <p>
 * // 创建SSLContext对象，并使用我们指定的信任管理器初始化
 * TrustManager[] tm = {new FileTrustManager()};
 * SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
 * sslContext.init(null,tm,new java.security.SecureRandom());
 * // 从上述SSLContext对象中得到SSLSocketFactory对象
 * SSLSocketFactory ssf=sslContext.getSocketFactory();
 * // 创建URL对象
 * URL myURL=new URL("https://ebanks.gdb.com.cn/sperbank/perbankLogin.jsp");
 * // 创建HttpsURLConnection对象，并设置其SSLSocketFactory对象
 * HttpsURLConnection httpsConn=(HttpsURLConnection)myURL.openConnection();
 * httpsConn.setSSLSocketFactory(ssf);
 * // 取得该连接的输入流，以读取响应内容
 * InputStreamReader insr=new InputStreamReader(httpsConn.getInputStream());
 * // 读取服务器的响应内容并显示
 * int respInt=insr.read();
 * while(respInt!=-1){
 * System.out.print((char)respInt);
 * respInt=insr.read();
 * }
 */

package cn.brainpoint.febs.libs.net.ssl;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

public class FileTrustManager implements X509TrustManager {
    /*
     * The default X509TrustManager returned by SunX509.  We'll delegate
     * decisions to it, and fall back to the logic in this class if the
     * default X509TrustManager doesn't trust it.
     */
    private X509TrustManager sunJSSEX509TrustManager;

    FileTrustManager() throws Exception {
        // create a "default" JSSE X509TrustManager.
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("trustedCerts"),
                "passphrase".toCharArray());
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("SunX509", "SunJSSE");
        tmf.init(ks);
        TrustManager tms[] = tmf.getTrustManagers();
        /*
         * Iterate over the returned trustmanagers, look
         * for an instance of X509TrustManager.  If found,
         * use that as our "default" trust manager.
         */
        for (int i = 0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager) {
                sunJSSEX509TrustManager = (X509TrustManager) tms[i];
                return;
            }
        }
        /*
         * Find some other way to initialize, or else we have to fail the
         * constructor.
         */
        throw new Exception("Couldn't initialize");
    }

    /**
     * get ssl socket factory for connect.
     * e.g. ((HttpsURLConnection)connect).setSSLSocketFactory(ssf);
     * @return socket factory.
     * @throws Exception cause in ssl exception
     */
    static public SSLSocketFactory getSocketFactory() throws Exception {
        TrustManager[] tm = {new FileTrustManager()};
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(null, tm, new java.security.SecureRandom());
        // 从上述SSLContext对象中得到SSLSocketFactory对象
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        return ssf;
    }


    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        try {
            sunJSSEX509TrustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException excep) {
            // do any special handling here, or rethrow exception.
        }
    }

    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        try {
            sunJSSEX509TrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException excep) {
            /*
             * Possibly pop up a dialog box asking whether to trust the
             * cert chain.
             */
//            throw excep;
        }
    }

    /**
     * Merely pass this through.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return sunJSSEX509TrustManager.getAcceptedIssuers();
    }
}