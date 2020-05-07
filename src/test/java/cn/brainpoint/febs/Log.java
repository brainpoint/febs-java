/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import com.sun.javafx.binding.StringFormatter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;

/**
 * @author pengxiang.li
 * @date 2020/1/31 8:04 下午
 */
public class Log {
    // 正确输出
    @Rule
    protected final static SystemOutRule logOut = new SystemOutRule();

    // 错误输出
    @Rule
    protected final static SystemErrRule logErr = new SystemErrRule();


    public static void out(String fmt, Object ...args) {
        String tid = "[tid: " + Thread.currentThread().getId() + "] ";
        System.out.printf(tid+fmt.concat("\r\n"), args);
        logOut.getLog();
    }
    public static void out(String msg) {
        String tid = "[tid: " + Thread.currentThread().getId() + "] ";
        System.out.print(tid + msg.concat("\r\n"));
        logOut.getLog();
    }

    public static void err(String fmt, Object ...args) {
        String tid = "[tid: " + Thread.currentThread().getId() + "] ";
        System.err.printf(tid+fmt.concat("\r\n"), args);
        logErr.getLog();
        Assert.fail(StringFormatter.format(fmt, args).get());
    }
    public static void err(String msg) {
        String tid = "[tid: " + Thread.currentThread().getId() + "] ";
        System.err.print(tid+msg.concat("\r\n"));
        logErr.getLog();
        Assert.fail(msg);
    }
}

