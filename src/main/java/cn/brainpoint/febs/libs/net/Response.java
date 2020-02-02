/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs.libs.net;

import java.util.List;
import java.util.Map;

/**
 * 响应结果
 *
 * @Author pengxiang.li
 * @Date 2020/1/31 7:24 下午
 */
public class Response {
    public Map<String, List<String>> headers;
    public String content;

    public Response() {
        this.content = "";
    }
}