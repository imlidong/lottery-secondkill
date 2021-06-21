package com.sankuai.mdp.lotterylidongwebservice.service;

import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;

import java.util.List;
import java.util.Map;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DrawService.java
 * @Description TODO
 * @createTime 2021年06月16日 15:08:00
 */
@ThriftService
public interface DrawService {
    @ThriftMethod
    List<String> queryLotteryResult();

    @ThriftMethod
    boolean initCache(int activityId);

    @ThriftMethod
    Map<Integer, Integer> countAllStock(String uuid);

    @ThriftMethod
    boolean deductStockCache(int prizeId);

    @ThriftMethod
    void sendMsg(String msg);

    @ThriftMethod
    String queryActivityTime(String uuid);

    @ThriftMethod
    void setIpInCache(String ip);

    @ThriftMethod
    boolean queryIpInCache(String ip);
}
