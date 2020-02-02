/**
 * Copyright (c) 2019 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2019-07-17 17:13
 * Desc: 无条件信任.
 */

package cn.brainpoint.febs.libs.net.ssl;

import javax.net.ssl.*;

public class NullHostNameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
}