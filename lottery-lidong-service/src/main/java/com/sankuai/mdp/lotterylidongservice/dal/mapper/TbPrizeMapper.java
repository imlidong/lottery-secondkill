package com.sankuai.mdp.lotterylidongservice.dal.mapper;


import com.sankuai.mdp.lotterylidongservice.dal.entity.TbPrize;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbPrizeMapper {
    List<TbPrize> selectByActivityID(@Param("activityId") int activityId);

    int deductStock(@Param("id") int id);

    TbPrize queryPrizeByID(@Param("id") int id);
}