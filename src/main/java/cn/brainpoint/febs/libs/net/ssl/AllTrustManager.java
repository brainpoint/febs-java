/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-17 17:13
 * Desc: 无条件信任.
 */

package cn.brainpoint.febs.libs.net.ssl;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class AllTrustManager implements X509TrustManager {

    /**
     * get ssl socket factory for connect.
     * e.g. ((HttpsURLConnection)connect).setSSLSocketFactory(ssf);
     * @return socket factory.
     * @throws Exception cause in ssl exception
     */
    static public SSLSocketFactory getSocketFactory() throws Exception {
        TrustManager[] tm = {new AllTrustManager()};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tm, new java.security.SecureRandom());
        // 从上述SSLContext对象中得到SSLSocketFactory对象
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        return ssf;
    }


    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    /**
     * Merely pass this through.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}