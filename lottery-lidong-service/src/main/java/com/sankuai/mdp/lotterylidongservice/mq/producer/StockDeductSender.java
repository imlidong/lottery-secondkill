package com.sankuai.mdp.lotterylidongservice.mq.producer;

import com.meituan.mafka.client.producer.AsyncProducerResult;
import com.meituan.mafka.client.producer.FutureCallback;
import com.meituan.mafka.client.producer.IProducerProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName StockDeductSender.java
 * @Description TODO
 * @createTime 2021年06月17日 15:07:00
 */
@Component
@Slf4j
public class StockDeductSender {
    @Autowired
    @Qualifier("stockProducer")
    private IProducerProcessor producerProcessor;

    public void send(String msg) {
        try {
            producerProcessor.sendAsyncMessage(msg, new FutureCallback() {
                @Override
                public void onSuccess(AsyncProducerResult asyncProducerResult) {
                    log.info("通知db扣减消息发送成功");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.info("通知db扣减消息发送失败", throwable);
                }
            });
        } catch (Exception e) {
            log.error("消息队列发送消息出现异常", e);
        }
    }
}
