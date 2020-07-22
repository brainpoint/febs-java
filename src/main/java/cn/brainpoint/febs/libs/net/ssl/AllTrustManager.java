/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-17 17:13
 * Desc: 无条件信任.
 */

package cn.brainpoint.febs.libs.net.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class AllTrustManager implements X509TrustManager {

    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    /**
     * Delegate to the default trust manager.
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    /**
     * Merely pass this through.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}