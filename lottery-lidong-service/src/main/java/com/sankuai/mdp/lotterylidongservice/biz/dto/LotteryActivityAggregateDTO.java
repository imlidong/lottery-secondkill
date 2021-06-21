package com.sankuai.mdp.lotterylidongservice.biz.dto;

import com.sankuai.mdp.lotterylidongservice.dal.entity.TbPrize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName LotteryActivity.java
 * @Description TODO
 * @createTime 2021年06月13日 22:03:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LotteryActivityAggregateDTO {
    private long activityNo; // 活动编号
    private Date onlineTime; // 上线时间
    private Date offlineTime; // 下线时间
    private List<TbPrize> prizeList; // 奖品列表
}
