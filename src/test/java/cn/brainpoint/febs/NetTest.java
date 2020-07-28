/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import cn.brainpoint.febs.libs.net.Request;
import cn.brainpoint.febs.libs.promise.IPromise;

/**
 * @author pengxiang.li
 */
public class NetTest {

    @Test
    public void testGet() {
        String tag = "Net get: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        Request req = new Request("https://www.baidu.com", "sdfsfddsf", "get");
        try {
            Febs.Net.fetch(req).then((res) -> {
                return res.text();
            }).then((res) -> {
                Log.out(tag + res);
            }).execute().get();
        } catch (ExecutionException e) {
            Log.err(tag + e.getMessage());
        }
    }

    @Test
    public void testText() {
        String tag = "Net text: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        ArrayList<IPromise> all = new ArrayList<>();

        for (int i = 0; i < 1; i++) {// code.
            Log.out(tag + " " + i);
            IPromise p = Febs.Net.fetch("https://www.baidu.com").then(res -> {
                // code.
                Log.out(tag + res.getStatusCode() + " " + res.getStatusMsg());

                // headers.
                Set<String> keySet = res.getHeaderKeySet();
                Log.out(tag + "header: " + keySet.size());
                Log.out(tag + "header: " + keySet.toString());
                Iterator<String> it1 = keySet.iterator();
                while (it1.hasNext()) {
                    String ID = it1.next();
                    Log.out(tag + "header: " + ID);
                    Collection<String> el = res.getHeaders(ID);
                    Log.out(tag + el);
                }

                return res.text();
            }).then(res -> {
                Log.out(tag + res);
                return res;
            }).fail((e) -> {
                e.printStackTrace(System.err);
                Log.err(tag + e.getMessage());
            }).finish(() -> {
                Log.out(tag + "dump: " + Promise.dumpDebug());
            });

            all.add(p);
        }

        try {
            String ret = (String) Promise.all(all).then((res) -> {
                Log.out(tag + " finish: " + res.toString());
                return res.toString();
            }).fail(e -> {
                e.printStackTrace();
            }).execute().get();

            Log.out(tag + " show in future", ret);
        } catch (Exception e) {
            Log.err(tag + e.getMessage());
        }
    }

    @Test
    public void testBlob() {
        String tag = "Net Blob: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        try {
            String ret = (String) Febs.Net.fetch("http://www.baidu.com").then(res -> {
                return res.blob();
            }).then((res) -> {
                BufferedReader in = (BufferedReader) res;
                char buf[] = new char[1024];

                while (in.read(buf, 0, buf.length) != -1) {
                    Log.out(tag + " %s ", Arrays.toString(buf));
                    Arrays.fill(buf, '\0');
                }

                // important to call close().
                in.close();

                return "success";
            }).fail((e) -> {
                Log.err(tag + e.getMessage());
            }).finish(() -> {
                Log.out(tag + "dump: " + Promise.dumpDebug());
            }).execute().get(1000, TimeUnit.MILLISECONDS);

            if (!"success".equals(ret)) {
                Log.err(tag + "return error");
            } else {
                Log.out(tag + "success");
            }
        } catch (TimeoutException e) {
            Log.err(tag + "timeout");
        } catch (ExecutionException e) {
            Log.err(tag + e.getMessage());
        }
    }
}
