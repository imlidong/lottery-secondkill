package com.sankuai.mdp.lotterylidongservice.dal.mapper;


import com.sankuai.mdp.lotterylidongservice.dal.entity.TbWinningRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbWinningRecordMapper {
    List<TbWinningRecord> queryLotteryResult();

    int insert(TbWinningRecord tbWinningRecord);
}