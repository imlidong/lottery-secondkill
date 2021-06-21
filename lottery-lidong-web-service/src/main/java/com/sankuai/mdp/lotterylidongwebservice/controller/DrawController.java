package com.sankuai.mdp.lotterylidongwebservice.controller;

import com.alibaba.fastjson.JSON;
import com.dianping.rhino.annotation.Degrade;
import com.dianping.rhino.annotation.Rhino;
import com.google.common.collect.Maps;
import com.meituan.mdp.boot.starter.thrift.annotation.MdpThriftClient;
import com.sankuai.mdp.lotterylidongwebservice.dto.DrawRequestDTO;
import com.sankuai.mdp.lotterylidongwebservice.dto.MsgDTO;
import com.sankuai.mdp.lotterylidongwebservice.enums.DrawResponse;
import com.sankuai.mdp.lotterylidongwebservice.service.DrawService;
import org.mortbay.log.Log;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName DrawController.java
 * @Description TODO
 * @createTime 2021年06月16日 14:16:00
 */
@ResponseBody
@RequestMapping("/activity")
@Rhino
public class DrawController {
    @MdpThriftClient(
            remoteAppKey = "com.sankuai.lotterylidong52",
            remoteServerPort = 8100,
            timeout = 1000
    )
    private DrawService drawService;

    // 这里可以优化。控制层应该是无状态的，如果运营修改了db，这里感知不到，所以可能回出现逻辑上的错误。
    // 所以可以把存在map的内容放到一个 中心化 的缓存处，每次从缓存拿，这样即使运营修改了，就可以更新缓存，拿到最新的。
    // 没有足够时间写，下次有时间了可以再优化。
    private final Map<String, Map<Integer, Integer>> countMap = Maps.newConcurrentMap();
    private final Map<String, String> timeMap = Maps.newConcurrentMap();

    /**
     * description: <p>
     * 参与抽奖的web接口, 加入rhino熔断降级
     * </p>
     *
     * @param drawRequestDTO: 抽奖携带的参数封装对象
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:04 下午
     * @return: java.lang.String
     */
    @Degrade(rhinoKey = "draw", fallBackMethod = "fallBack")
    @PostMapping("/draw")
    public DrawResponse draw(@RequestBody @NotNull DrawRequestDTO drawRequestDTO) {
        String uuid = drawRequestDTO.getUuid();
        String ip = drawRequestDTO.getIp();
        if(drawService.queryIpInCache(ip)){
            return DrawResponse.FAIL;
        }
        drawService.setIpInCache(ip);
        // 时间不符合条件，直接返回
        DrawResponse drawResponse = checkDate(uuid);
        if (DrawResponse.TIME_OUT.equals(drawResponse) || DrawResponse.NOT_ARRIVE_TIME.equals(drawResponse)) {
            return drawResponse;
        }
        // 时间符合条件，开始抽奖
        int choosePrize = getChoosePrize(uuid);
        // 落到中奖区间, 先扣缓存
        if (choosePrize != 0) {
            boolean success = drawService.deductStockCache(choosePrize);
            if (success) {
                // 异步通知 db 扣减
                drawService.sendMsg(JSON.toJSONString(new MsgDTO(drawRequestDTO.getIp(), choosePrize)));
                return DrawResponse.SUCCESS;
            }else{
                return DrawResponse.NO_STOCK;
            }
        }
        return DrawResponse.FAIL;
    }

    public DrawResponse fallBack(@RequestBody @NotNull DrawRequestDTO drawRequestDTO){
        return DrawResponse.DANG;
    }


    private DrawResponse checkDate(String uuid) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        String time = "";
        if (timeMap.containsKey(uuid)) {
            time = timeMap.get(uuid);
        } else {
            // 把活动的时间缓存起来，后面查询可以不走db
            time = drawService.queryActivityTime(uuid);
            timeMap.put(uuid, time);
        }
        try {
            Date startTime = sf.parse(time.split(",")[0]);
            Date endTime = sf.parse(time.split(",")[1]);
            Date now = new Date();
            if (now.before(startTime)) {
                return DrawResponse.NOT_ARRIVE_TIME;
            } else if (now.after(endTime)) {
                return DrawResponse.TIME_OUT;
            }
        } catch (ParseException e) {
            Log.info("时间解析出错");
        }
        return null;
    }

    /**
     * description: <p>
     * 通过算法得到 最后是否中奖 0未中奖,1 2 3代表1 2 3等奖
     * </p>
     *
     * @param uuid:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:07 下午
     * @return: int
     */

    private int getChoosePrize(@NotNull String uuid) {
        // 查询得到活动的 所有奖品的map，为后面算法提供基础
        Map<Integer, Integer> map;
        if (!countMap.containsKey(uuid)) {
            map = drawService.countAllStock(uuid);
            countMap.put(uuid, map);
        }
        map = countMap.get(uuid);

        //统计 奖品总权重
        int totalWeight = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            totalWeight += entry.getValue();
        }
        //生成一个随机数
        int randNum = new Random().nextInt(totalWeight);
        int prev = 0;
        int choosePrize = 0;
        // 按照权重计算中奖区间
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (randNum >= prev && randNum < prev + entry.getValue()) {
                choosePrize = entry.getKey();
                break;
            }
            prev += entry.getValue();
        }
        return choosePrize;
    }

    /**
     * description: <p>
     * 初始化db库存到缓存的接口
     * </p>
     *
     * @param activityId:
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:04 下午
     * @return: java.lang.String
     */

    @PostMapping("/initCache")
    public String initCache(@RequestParam("activityId") @NotNull int activityId) {
        boolean resp = drawService.initCache(activityId);
        if (resp) {
            return "success";
        }
        return "fail";
    }

    /**
     * description: <p>
     * 查询所有的中奖纪录
     * </p>
     *
     * @since: 1.0.0
     * @author: lidong52
     * @date: 2021/6/18 9:05 下午
     * @return: java.util.List<java.lang.String>
     */

    @GetMapping("/result")
    public List<String> loadResult() {
        return drawService.queryLotteryResult();
    }

}
