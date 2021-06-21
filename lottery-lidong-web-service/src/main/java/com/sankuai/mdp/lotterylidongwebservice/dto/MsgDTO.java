package com.sankuai.mdp.lotterylidongwebservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName MsgDTO.java
 * @Description TODO
 * @createTime 2021年06月17日 15:13:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgDTO implements Serializable {
    private String ip;
    private int prizeId;
}
