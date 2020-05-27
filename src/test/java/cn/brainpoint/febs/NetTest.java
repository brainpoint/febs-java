/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import cn.brainpoint.febs.libs.promise.IPromise;

/**
 * @author pengxiang.li
 * @date  2020/1/31 7:16 下午
 */
public class NetTest {
    @Test
    public void testText() {
        String tag = "Net text: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        ArrayList<IPromise> all = new ArrayList();

        for (int i = 0; i < 100; i++) {// code.
            Log.out(tag + " " + i);
            IPromise p = Febs.Net.fetch("https://www.baidu.com")
                    .then(res -> {
                        // code.
                        Log.out(tag + res.statusCode + " " + res.statusMsg);

                        // headers.
                        Set<String> keySet = res.headers.keySet();
                        Iterator<String> it1 = keySet.iterator();
                        while (it1.hasNext()) {
                            String ID = it1.next();
                            Log.out(tag + "header: " + ID);
                            List<String> el = res.headers.get(ID);
                            Log.out(tag + el);
                        }

                        return res.text();
                    })
                    .then(res -> {
                        Log.out(tag + res);
                    })
                    .fail((e) -> {
                        e.printStackTrace(System.err);
                        Log.err(tag + e.getMessage());
                    })
                    .finish(() -> {
                        Log.out(tag + "dump: " + Promise._dumpDebug());
                    });

            all.add(p);
        }

        Promise.join(Promise.all(all).then(()->{
            Log.out(tag + " finish");
        }).fail(e->{
            e.printStackTrace();
        }));
    }

//    @Test
    public void testBlob() {
        String tag = "Net Blob: ";
        Log.out("========================================");
        Log.out(tag + "begin");

        Febs.Net.fetch("http://www.baidu.com")
                .then(res->{
                    return res.blob();
                })
                .then((res)->{
                    BufferedReader in = (BufferedReader)res;
                    char buf[] = new char[1024];

                    while (in.read(buf, 0, buf.length) != -1) {
                        Log.out(tag + " %s ",  Arrays.toString(buf));
                        Arrays.fill(buf, '\0');
                    }

                    // important to call close().
                    in.close();
                })
                .fail((e)->{
                    Log.err(tag+e.getMessage());
                })
                .finish(()->{
                    Log.out(tag+"dump: "+Promise._dumpDebug());
                })
                .execute();
    }
}
