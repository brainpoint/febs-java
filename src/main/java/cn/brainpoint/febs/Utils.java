/*
 * Copyright (c) 2020 Copyright bp All Rights Reserved.
 * Author: pengxiang.li
 * Desc:
 */

package cn.brainpoint.febs;

import cn.brainpoint.febs.libs.promise.IResolveNoRet;

/**
 * @author pengxiang.li
 * @date  2020/2/2 3:43 下午
 */
public class Utils {

    public Utils() {

    }

    /**
     * Sleep in promise way.
     * <i>e.g.</i>
     * <code>
     *     febs.Utils.sleep(1000).then(()-&gt;{
     *          // Will call in 1000ms.
     *     });
     * </code>
     *
     * @param millisecond sleep time.
     * @return Promise object
     */
    public Promise<Void> sleep(long millisecond) {
        return new Promise<Void>((resolve, reject)->{
            try {
                Thread.sleep(millisecond);
            }
            catch (Exception e) {
                reject.execute(e);
                return;
            }
            resolve.execute(null);
        });
    }
}
