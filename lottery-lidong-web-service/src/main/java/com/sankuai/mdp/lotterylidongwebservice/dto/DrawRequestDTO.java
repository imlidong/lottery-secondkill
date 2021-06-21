package com.sankuai.mdp.lotterylidongwebservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DrawRequestDTO.java
 * @Description TODO
 * @createTime 2021年06月16日 22:19:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrawRequestDTO implements Serializable {
    private String ip;  // 请求的ip地址 抽奖者的标识
    private String uuid;  // 表示此次参加的抽奖活动uuid
}
