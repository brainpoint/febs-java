/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

/**
 * @Author pengxiang.li
 * @Date 2020/2/2 3:43 下午
 */
public class Utils {

    /**
     * @e.g.
     *     febs.Utils.sleep(1000).then(()=>{
     *          //1000ms之后resolve.
     *     });
     * @param millisecond
     * @return
     */
    public static Promise sleep(long millisecond) {
        return new Promise((resolve, reject)->{
            Thread.sleep(millisecond);
        });
    }


}
