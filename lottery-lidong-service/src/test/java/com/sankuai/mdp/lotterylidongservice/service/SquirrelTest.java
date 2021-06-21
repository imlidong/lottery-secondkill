package com.sankuai.mdp.lotterylidongservice.service;

import com.dianping.squirrel.client.StoreKey;
import com.dianping.squirrel.client.impl.redis.RedisStoreClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName Squirrel.java
 * @Description TODO
 * @createTime 2021年06月16日 19:11:00
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SquirrelTest {
    @Autowired(required = false)
    @Qualifier(value = "redisClient0")
    private RedisStoreClient redisStoreClient;

    @Test
    public void testSquirrel(){
        StoreKey storeKey = new StoreKey("future_plan_lidong52",1);
        boolean res = redisStoreClient.set(storeKey, 10);
        Assert.assertTrue(res);
    }

    @Test
    public void testQueryIpInCache(){
        StoreKey storeKey = new StoreKey("future_plan_lidong52-ip","1234");
        Long value = redisStoreClient.get(storeKey);
        System.out.println(value);
    }
}
