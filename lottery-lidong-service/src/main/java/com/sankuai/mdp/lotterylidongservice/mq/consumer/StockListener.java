package com.sankuai.mdp.lotterylidongservice.mq.consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.meituan.mafka.client.consumer.ConsumeStatus;
import com.meituan.mdp.boot.starter.mafka.consumer.anno.MdpMafkaMsgReceive;
import com.sankuai.mdp.lotterylidongapi.service.DrawService;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbGoods;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbPrize;
import com.sankuai.mdp.lotterylidongservice.dal.entity.TbWinningRecord;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbGoodsMapper;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbPrizeMapper;
import com.sankuai.mdp.lotterylidongservice.dal.mapper.TbWinningRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.mortbay.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName StockListener.java
 * @Description TODO
 * @createTime 2021年06月16日 20:18:00
 */
@Slf4j
@Service("stockListener")
public class StockListener {
    @Autowired
    private DrawService drawService;

    /**
     * description: <p>消息监听 扣减库存</p>
     *
     * @param msgBody:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/17 10:41 下午
     * @return: com.meituan.mafka.client.consumer.ConsumeStatus
     */

    @MdpMafkaMsgReceive(customSubscribeGroup = "prize-stock")
    protected ConsumeStatus onRecvMessage(String msgBody) {
        JsonObject jsonObject = new JsonParser().parse(msgBody).getAsJsonObject();
        String ip = jsonObject.get("ip").getAsString();
        int prizeId = jsonObject.get("prizeId").getAsInt();
        try {
            drawService.deductStockAndGenerateDoc(ip, prizeId);
        } catch (Exception e) {
            log.info("出现异常，进行事务回滚", e);
            drawService.rollBackForCache(prizeId);
        }
        return ConsumeStatus.CONSUME_SUCCESS;
    }

}
