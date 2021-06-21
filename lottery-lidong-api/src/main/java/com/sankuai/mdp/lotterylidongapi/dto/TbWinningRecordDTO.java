package com.sankuai.mdp.lotterylidongapi.dto;

import lombok.*;

import java.util.Date;

/**
 * 表名: TB_WinningRecord
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TbWinningRecordDTO {
    /**
     * 字段: ID
     * 说明: 主键ID
     */
    private Integer id;

    /**
     * 字段: ActivityID
     * 说明: 活动ID
     */
    private Integer activityId;

    /**
     * 字段: PrizeID
     * 说明: 奖品ID
     */
    private Integer prizeId;

    /**
     * 字段: GoodsID
     * 说明: 物品ID
     */
    private Integer goodsId;

    /**
     * 字段: GoodsName
     * 说明: 物品名称
     */
    private String goodsName;

    /**
     * 字段: IP
     * 说明: 中奖ip地址
     */
    private String ip;

    /**
     * 字段: LotteryTime
     * 说明: 中奖时间
     */
    private Date lotteryTime;
}