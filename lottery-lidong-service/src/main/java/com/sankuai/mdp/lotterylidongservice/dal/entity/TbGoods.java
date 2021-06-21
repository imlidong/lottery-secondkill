package com.sankuai.mdp.lotterylidongservice.dal.entity;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * TB_Goods
 * @author lidong52
 */
@Data
@Builder
public class TbGoods implements Serializable {
    /**
     * 主键ID
     */
    private Integer ID;

    /**
     * 物品名称
     */
    private String goodsName;

    /**
     * 物品价值
     */
    private Integer price;

    /**
     * 图片列表
     */
    private String imageList;

    private static final long serialVersionUID = 1L;
}