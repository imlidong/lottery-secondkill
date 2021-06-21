package com.sankuai.mdp.lotterylidongservice.service;

import com.meituan.mafka.client.producer.AsyncProducerResult;
import com.meituan.mafka.client.producer.FutureCallback;
import com.meituan.mafka.client.producer.IProducerProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName MafkaTest.java
 * @Description TODO
 * @createTime 2021年06月16日 20:03:00
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class MafkaTest {
    @Autowired
    @Qualifier("stockProducer")
    private IProducerProcessor producerProcessor;

    @Test
    public void testSendMsg(){
        try {
            producerProcessor.sendAsyncMessage("12325", new FutureCallback() {
                @Override
                public void onSuccess(AsyncProducerResult asyncProducerResult) {
                    System.out.println("测试发送成功");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    System.out.println("测试发送失败");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
