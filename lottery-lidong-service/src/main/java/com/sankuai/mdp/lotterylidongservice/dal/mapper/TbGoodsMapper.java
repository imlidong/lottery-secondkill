package com.sankuai.mdp.lotterylidongservice.dal.mapper;

import com.sankuai.mdp.lotterylidongservice.dal.entity.TbGoods;
import org.springframework.stereotype.Repository;

@Repository
public interface TbGoodsMapper {
    int deleteByPrimaryKey(Integer ID);

    int insert(TbGoods record);

    int insertSelective(TbGoods record);

    TbGoods selectByPrimaryKey(Integer ID);

    int updateByPrimaryKeySelective(TbGoods record);

    int updateByPrimaryKey(TbGoods record);
}