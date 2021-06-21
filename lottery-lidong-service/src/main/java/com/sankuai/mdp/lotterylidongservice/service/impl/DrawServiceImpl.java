package com.sankuai.mdp.lotterylidongservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.dianping.squirrel.client.StoreKey;
import com.dianping.squirrel.client.impl.redis.RedisStoreClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meituan.mdp.boot.starter.thrift.annotation.MdpThriftServer;
import com.sankuai.mdp.lotterylidongapi.dto.TbWinningRecordDTO;
import com.sankuai.mdp.lotterylidongapi.service.DrawService;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbGoods;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbLotteryActivity;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbPrize;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbWinningRecord;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbGoodsMapper;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbLotteryActivityMapper;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbPrizeMapper;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbWinningRecordMapper;
import com.sankuai.mdp.lotterylidongservice.exception.DuplicateIPException;
import com.sankuai.mdp.lotterylidongservice.mq.producer.StockDeductSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.sankuai.mdp.lotterylidongservice.constant.Constant;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DrawServiceImpl.java
 * @Description TODO
 * @createTime 2021年06月13日 11:47:00
 */
@Slf4j
@Service
@MdpThriftServer(
        port = 8100
)
public class DrawServiceImpl implements DrawService {
    @Autowired
    private TbPrizeMapper tbPrizeMapper;

    @Autowired
    private TbWinningRecordMapper tbWinningRecordMapper;

    @Autowired
    @Qualifier(value = "redisClient0")
    private RedisStoreClient redisStoreClient;

    @Autowired
    private StockDeductSender stockDeductSender;

    @Autowired
    private TbGoodsMapper tbGoodsMapper;

    @Autowired
    private TbLotteryActivityMapper tbLotteryActivityMapper;

    /**
     * description: <p>
     * 在活动开始前将 db 库存全量同步到 缓存中
     * </p>
     *
     * @param activityId:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 1:48 下午
     * @return: boolean
     */

    @Override
    public boolean initCache(int activityId) {
        List<TbPrize> tbPrizeList = tbPrizeMapper.selectByActivityID(activityId);
        for (TbPrize tbPrize : tbPrizeList) {
            StoreKey key = new StoreKey(Constant.CACHE_CATEGORY, tbPrize.getId());
            // 先删除所有key，再重新将db记录刷入 缓存
            redisStoreClient.delete(key);
            redisStoreClient.increase(key, tbPrize.getStock());
        }
        return true;
    }

    /**
     * description: <p>
     * 查询中奖纪录
     * </p>
     *
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 1:47 下午
     * @return: java.util.List<java.lang.String>
     */

    @Override
    public List<String> queryLotteryResult() {
        List<TbWinningRecord> tbWinningRecordList = tbWinningRecordMapper.queryLotteryResult();
        List<String> list = Lists.newArrayListWithExpectedSize(tbWinningRecordList.size());
        for (TbWinningRecord record : tbWinningRecordList) {
            TbWinningRecordDTO recordDTO = TbWinningRecordDTO.builder()
                    .activityId(record.getActivityId())
                    .goodsId(record.getGoodsId())
                    .goodsName(record.getGoodsName())
                    .id(record.getId())
                    .ip(record.getIp())
                    .prizeId(record.getPrizeId())
                    .lotteryTime(record.getLotteryTime())
                    .build();
            list.add(JSON.toJSONString(recordDTO));
        }
        return list;
    }


    /**
     * description: <p>
     * 根据活动id 来查询所有奖品，返回id-权重map
     * </p>
     *
     * @param uuid:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 1:27 下午
     * @return: java.util.Map<java.lang.Integer, java.lang.Integer>
     */

    @Override
    public Map<Integer, Integer> countAllStock(String uuid) {
        int activityId = tbLotteryActivityMapper.selectIDByUuid(uuid);
        List<TbPrize> tbPrizeList = tbPrizeMapper.selectByActivityID(activityId);
        Map<Integer, Integer> map = Maps.newLinkedHashMap();
        for (TbPrize tbPrize : tbPrizeList) {
            map.put(tbPrize.getId(), tbPrize.getProbability());
        }
        return map;
    }


    /**
     * description: <p>
     * 获取分布式锁扣减缓存中的库存
     * </p>
     *
     * @param prizeId:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 1:49 下午
     * @return: boolean
     */

    @Override
    public boolean deductStockCache(int prizeId) {
        final StoreKey lockKey = new StoreKey(Constant.CACHE_CATEGORY, "-1");
        StoreKey key = new StoreKey(Constant.CACHE_CATEGORY, prizeId);
        try {
            long stock = redisStoreClient.get(key);
            if (stock > 0) {
                // 尝试获取 分布式锁
                boolean lockSuccess = redisStoreClient.setnx(lockKey, 1, 5);
                int tryCount = 1;
                // 如果没有获取到，最多再自旋5次，否则直接返回失败
                while (!lockSuccess && tryCount <= 5) {
                    lockSuccess = redisStoreClient.setnx(lockKey, 1, 5);
                    tryCount += 1;
                }
                // 尝试后没获取到锁
                if (!lockSuccess) {
                    return false;
                } else {
                    // 获取到了分布式锁
                    stock = redisStoreClient.get(key);
                    // double check,因为可能在等待得到锁的过程中，stock已经被修改了
                    if (stock <= 0) {
                        redisStoreClient.delete(lockKey);
                        return false;
                    } else {
                        // 扣减缓存库存
                        redisStoreClient.decrBy(key, 1L);
                        // 释放锁
                        redisStoreClient.delete(lockKey);
                        return true;
                    }
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("缓存获取锁过程出现异常", e);
        } finally {
            // 最终确保锁的释放
            redisStoreClient.delete(lockKey);
        }
        return false;
    }

    /**
     * description: <p>
     * 异步消息通知db扣减库存
     * </p>
     *
     * @param msg:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 1:46 下午
     * @return: void
     */

    @Override
    public void sendMsg(String msg) {
        stockDeductSender.send(msg);
    }


    @Transactional(transactionManager = "txManager", rollbackFor = Exception.class)
    public void deductStockAndGenerateDoc(String ip, int prizeId) {
        try {
            // 扣减库存
            int rowUpdate = tbPrizeMapper.deductStock(prizeId);
            // 如果扣减成功，则落中奖纪录
            if (rowUpdate > 0) {
                TbPrize tbPrize = tbPrizeMapper.queryPrizeByID(prizeId);
                Integer goodsId = tbPrize.getGoodsId();
                // 根据goodsId查询goods记录
                TbGoods tbGoods = tbGoodsMapper.selectByPrimaryKey(goodsId);
                int rowNum = tbWinningRecordMapper.insert(new TbWinningRecord(0, tbPrize.getActivityId(), prizeId, goodsId, tbGoods.getGoodsName(), ip, new Date()));
                // 直接利用数据库的 唯一索引 做幂等
                if (rowNum <= 0) {
                    throw new DuplicateIPException();
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * description: <p>
     * 回滚 缓存 库存
     * </p>
     *
     * @param prizeId:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:26 下午
     * @return: void
     */

    @Override
    public void rollBackForCache(int prizeId) {
        StoreKey storeKey = new StoreKey(Constant.CACHE_CATEGORY, prizeId);
        redisStoreClient.incrBy(storeKey, 1L);
    }

    /**
     * description: <p>
     * 根据活动id 来查询开始结束时间
     * </p>
     *
     * @param uuid:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:27 下午
     * @return: java.util.Map<java.lang.Integer, java.lang.String>
     */

    @Override
    public String queryActivityTime(String uuid) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        TbLotteryActivity tbLotteryActivity = tbLotteryActivityMapper.selectByUuid(uuid);
        return dateFormat.format(tbLotteryActivity.getStartTime()) + "," + dateFormat.format(tbLotteryActivity.getEndTime());
    }

    @Override
    public void setIpInCache(String ip) {
        StoreKey storeKey = new StoreKey(Constant.CACHE_CATEGORY_IP, ip);
        redisStoreClient.set(storeKey, 1L);
        // 设置10s的过期时间
        redisStoreClient.expire(storeKey, 10);
    }

    @Override
    public boolean queryIpInCache(String ip) {
        StoreKey storeKey = new StoreKey(Constant.CACHE_CATEGORY_IP, ip);
        Long value = redisStoreClient.get(storeKey);
        return value != null;
    }
}
