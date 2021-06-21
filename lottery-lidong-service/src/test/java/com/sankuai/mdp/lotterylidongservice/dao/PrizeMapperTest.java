package com.sankuai.mdp.lotterylidongservice.dao;

import com.sankuai.mdp.lotterylidongservice.dal.entity.TbPrize;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbPrizeMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName PrizeMapperTest.java
 * @Description TODO
 * @createTime 2021年06月17日 00:46:00
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class PrizeMapperTest {
    @Autowired
    private TbPrizeMapper tbPrizeMapper;

    @Test
    public void testSelectByID() {
        List<TbPrize> tbPrizeList = tbPrizeMapper.selectByActivityID(1);
        Assert.assertNotNull(tbPrizeList);
        System.out.println(tbPrizeList);
    }

    @Test
    public void testDeduct() {
        int res = tbPrizeMapper.deductStock(3);
        System.out.println(res);
    }

}
