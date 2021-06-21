package com.sankuai.mdp.lotterylidongwebservice.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DrawResponse.java
 * @Description TODO
 * @createTime 2021年06月18日 20:55:00
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DrawResponse {
    SUCCESS(100001, "参与抽奖成功，待活动结束查看中奖结果"),
    FAIL(200001, "很遗憾，未中奖"),
    NOT_ARRIVE_TIME(300001, "抽奖未开始"),
    TIME_OUT(400001, "抽奖已结束"),
    NO_STOCK(500001, "无奖品库存"),
    DANG(600001,"系统开小差，服务不可用");

    int code;
    String desc;

    DrawResponse(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
