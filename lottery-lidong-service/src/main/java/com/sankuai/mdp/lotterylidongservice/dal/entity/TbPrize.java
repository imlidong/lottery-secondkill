package com.sankuai.mdp.lotterylidongservice.dal.entity;

import lombok.*;

/**
 *
 *   表名: TB_Prize
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TbPrize {
    /**
     *   字段: ID
     *   说明: 主键ID
     */
    private Integer id;

    /**
     *   字段: ActivityID
     *   说明: 对应的活动ID
     */
    private Integer activityId;

    /**
     *   字段: GoodsID
     *   说明: 对应的物品ID
     */
    private Integer goodsId;

    /**
     *   字段: PrizeLevel
     *   说明: 几等奖
     */
    private Integer prizeLevel;

    /**
     *   字段: Probability
     *   说明: 中奖概率，代码逻辑中除以10000
     */
    private Integer probability;

    /**
     *   字段: Stock
     *   说明: 库存量
     */
    private Integer stock;
}