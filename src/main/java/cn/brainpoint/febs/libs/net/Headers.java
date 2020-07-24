/**
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: lipengxiang
 * Date: 2020-2020/6/2 15:17
 * Desc:
 */
package cn.brainpoint.febs.libs.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Headers {

    protected Map<String, List<String>> headerSet;

    public Headers() {
    }

    public Headers(Map<String, List<String>> headerSet) {
        this.headerSet = headerSet;
    }

    /**
     * Get the key size in the headers
     * 
     * @return key size of headers
     */
    public int getHeaderLength() {
        return this.headerSet == null ? 0 : this.headerSet.size();
    }

    /**
     * Get header key set.
     * 
     * @return key set
     */
    public Set<String> getHeaderKeySet() {
        if (this.headerSet == null) {
            return new HashSet<>();
        } else {
            return this.headerSet.keySet();
        }
    }

    /**
     * Get header by key.
     * 
     * @param key header key
     * @return header value collection
     */
    public Collection<String> getHeaders(String key) {
        if (this.headerSet == null) {
            return new ArrayList<>();
        }
        key = upperCaseFirst(key);
        if (!this.headerSet.containsKey(key)) {
            return new ArrayList<>();
        }

        return this.headerSet.get(key);
    }

    /**
     * Get first header by key.
     * 
     * @param key header key
     * @return header value
     */
    public String getHeader(String key) {
        if (this.headerSet == null) {
            return null;
        }
        key = upperCaseFirst(key);
        if (!this.headerSet.containsKey(key)) {
            return null;
        }

        Collection<String> values = this.headerSet.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }

    /**
     * Set header by key and value.
     * 
     * @param key   will capitalize the conversion first letter; e.g.
     *              "x-custom-header" -&gt; "X-Custom-Header"
     * @param value value of header
     */
    public void setHeader(String key, String value) {
        if (this.headerSet == null) {
            this.headerSet = new HashMap<>();
        }

        key = upperCaseFirst(key);

        Collection<String> headers1 = this.headerSet.get(key);
        if (null == headers1) {
            ArrayList<String> values = new ArrayList<>();
            values.add(value);
            this.headerSet.put(key, values);
        } else {
            List<String> headers2 = (List<String>) headers1;
            if (headers2.size() == 1) {
                headers2.set(0, value);
            } else {
                headers2.clear();
                headers2.add(value);
            }
        }
    }

    /**
     * add header by key and value.
     * 
     * @param key   will capitalize the conversion first letter; e.g.
     *              "x-custom-header" -&gt; "X-Custom-Header"
     * @param value value of header
     */
    public void addHeader(String key, String value) {
        if (this.headerSet == null) {
            this.headerSet = new HashMap<>();
        }

        key = upperCaseFirst(key);
        Collection<String> headers1 = this.headerSet.get(key);
        if (null == headers1) {
            ArrayList<String> values = new ArrayList<>();
            values.add(value);
            this.headerSet.put(key, values);
        } else {
            List<String> headers2 = (List<String>) headers1;
            headers2.add(value);
        }
    }

    /**
     * Set header by key and values
     * 
     * @param key   will capitalize the conversion first letter; e.g.
     *              "x-custom-header" -&gt; "X-Custom-Header"
     * @param value value of header
     */
    public void setHeader(String key, Collection<String> value) {
        if (this.headerSet == null) {
            this.headerSet = new HashMap<>();
        }
        key = upperCaseFirst(key);
        Collection<String> headers1 = this.headerSet.get(key);
        if (null == headers1) {
            this.headerSet.put(key, new ArrayList<>(value));
        } else {
            this.headerSet.replace(key, new ArrayList<>(value));
        }
    }

    /**
     * Remove header by key
     * 
     * @param key header key
     */
    public void removeHeader(String key) {
        if (this.headerSet != null) {
            key = upperCaseFirst(key);
            this.headerSet.remove(key);
        }
    }

    private String upperCaseFirst(String headerKey) {
        String[] arr = headerKey.split("-");
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            if (sb.length() > 0) {
                sb.append("-");
            }

            char c = s.charAt(0);
            if (Character.isLowerCase(c) || Character.isUpperCase(c)) {
                sb.append(Character.toUpperCase(c));
                sb.append(s.substring(1).toLowerCase());
            } else {
                sb.append(s.toLowerCase());
            }
        }

        return sb.toString();
    }
}
