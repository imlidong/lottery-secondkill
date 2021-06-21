package com.sankuai.mdp.lotterylidongservice.dal.entity;

import java.util.Date;

import lombok.*;

/**
 *
 *   表名: TB_LotteryActivity
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TbLotteryActivity {
    /**
     *   字段: ID
     *   说明: 活动主键ID
     */
    private Integer id;

    /**
     *   字段: ActiveStatus
     *   说明: 活动状态
     */
    private Integer activeStatus;

    /**
     *   字段: StartTime
     *   说明: 开始时间
     */
    private Date startTime;

    /**
     *   字段: EndTime
     *   说明: 结束时间
     */
    private Date endTime;

    /**
     *   字段：uuid
     *   说明: uuid
     */
    private String uuid;
}