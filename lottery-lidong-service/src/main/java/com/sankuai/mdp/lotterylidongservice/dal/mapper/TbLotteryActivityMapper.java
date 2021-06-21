package com.sankuai.mdp.lotterylidongservice.dal.mapper;


import com.sankuai.mdp.lotterylidongservice.dal.entity.TbLotteryActivity;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TbLotteryActivityMapper {
    TbLotteryActivity selectByID(@Param("id") Integer id);

    TbLotteryActivity selectByUuid(@Param("uuid") String uuid);

    int selectIDByUuid(@Param("uuid") String uuid);
}