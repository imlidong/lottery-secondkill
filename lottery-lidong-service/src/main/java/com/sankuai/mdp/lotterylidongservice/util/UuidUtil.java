package com.sankuai.mdp.lotterylidongservice.util;

import java.util.UUID;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName UuidUtil.java
 * @Description TODO
 * @createTime 2021年06月21日 14:10:00
 */
public class UuidUtil {
    public static void main(String[] args) {
        String uuid = UUID.randomUUID().toString();
        System.out.println(uuid.replace("-",""));
    }
}
