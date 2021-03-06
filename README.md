# lottery-secondkill
抽奖+秒杀系统


## 一、抽奖算法的设计实现

### 算法思想

给每种奖品都有一个**权重**，对应一个区间，若落入该区间就表示中奖，那么通过动态调整区间大小就可改变获奖概率，即调整权重值即可

<img src="https://z3.ax1x.com/2021/06/19/RCnf3Q.png" alt="RCnf3Q.png" style="zoom:50%;" />

```java
randNum = new Random().nextInt(totalWeight);  // totalWeight = 上面权重列之和
```

然后通过random一个随机数，来判断落入了哪个区间即可。

```java
randNum = 8944 落在未中奖区间 未中奖 
randNum = 944 落在1元区间 中了一元
```

**总结**：这个抽奖算法其实就是通过调整各个奖项的权重来调整中奖的概率，因为是考虑瞬时高流量的抽奖，所以还涉及库存问题，因此即**是否中奖**除了落入区间外，还需判断减库存是否成功，如果减库存失败 仍当做未中奖，关于库存扣减见下面内容。

### 二、实体类设计
![image](https://user-images.githubusercontent.com/38515687/122766331-f9741980-d2d3-11eb-98d2-2a429e3651eb.png)


## 三、库存扣减的逻辑

### 扣减逻辑整体描述

考虑到瞬时抽奖的流量是非常高的，因此对于库存系统来说是个很大的考验。系统主要使用了redis缓存，kafka消息队列中间件等避免大量请求直接落到db，进而导致崩溃。因此，我们首先在活动开始前，将db的库存**全量同步**都缓存当中，先扣减缓存，再异步通知db扣减。

以下是一个请求到来后的逻辑（假设已通过抽奖算法落入某个奖项的区间了）：

1. 首先做时间的校验，时间节点可以缓存起来，这样不用每次都走db查，不然肯定奔溃。运营那边修改了时间后，可以再更新缓存。逻辑上，如果没到时间，返回活动未开始；如果超过时间，返回活动已结束。
![image](https://user-images.githubusercontent.com/38515687/122765309-f75d8b00-d2d2-11eb-9d1a-b914ddf867d9.png)


2. 缓存扣减

   首先从**缓存**中通过奖品ID获取到某个奖品的库存，如果库存小于等于0了（无库存），返回没有足够库存的响应。如果奖品库存大于0（库存存在），那么该线程通过自旋的方式去获取**分布式锁**（最多自旋5次，防止cpu做无用消耗），如果没有获取到锁，直接返回不中奖；如果获取到锁，再次做库存的校验（二次检查，因为中间可能被人修改了），库存仍旧足够的情况下，进行缓存的**原子扣减**，然后通过kafka异步的给db发送一条扣减消息。
   ![image](https://user-images.githubusercontent.com/38515687/122765377-05aba700-d2d3-11eb-8f59-711e367d8ef7.png)


3. db扣减

   kafka监听到扣减消息之后，要执行库存的扣减。

   但假如库存只剩1个了，有10个用户同时落入一元区间，如何避免`1-10=-9`的情况呢？

   库存扣减使用以下sql进行扣减（也可以使用数据库的乐观锁）

   ```xml
   <update id="deductStock" parameterType="java.util.Map">
       update TB_Prize
       set `Stock` = `Stock`-1
       where `ID`= #{id} and <![CDATA[ `Stock`> 0]]>
   </update>
   ```

   同时，在扣减完库存之后，要落一个中奖纪录到另外一个表中，**只有扣库存和记录落表之后，才算是真正的中奖了**。

   ```xml
   <insert id="insert" parameterType="java.util.Map">
       insert into TB_WinningRecord
       (`ID`, `ActivityID`, `PrizeID`,`GoodsID`,`GoodsName`,`IP`,`LotteryTime`)
       values
       (#{id},#{activityId},#{prizeId},#{goodsId},#{goodsName},#{ip},NOW())
   </insert>
   ```

4. db扣减失败的情况

   db有可能出现扣减失败的情况，这样子就不能落中奖纪录，因此需要把**扣db库存和落中奖纪录两个操作放到一个事务当中**。如果出现失败，那么进行事务的回滚。同时，因为失败了，缓存也需要更新，这个时候通过缓存的**原子加**操作进行对应**缓存库存的回滚补偿**，保证缓存和db的一致性

5. 如何防止同一个ip用户重复中奖

   这里用了简单的思想，使用数据库ip字段建立**唯一索引做了幂等**，如果已经有中奖纪录，那么会返回失败，这样就触发了事务回滚，也不会再中奖成功。

6. 下游服务突然挂了

   使用了Rhino进行降级熔断

   ![RC3i5t.png](https://z3.ax1x.com/2021/06/19/RC3i5t.png)

7. web接口限流

   使用Rhino的统一接口进行限流

   ![RC3IRf.png](https://z3.ax1x.com/2021/06/19/RC3IRf.png)
   
8. ip限制

   为了防止同一个IP地址连续重复的发起请求，增加中奖概率，需要对IP进行限制，主要采用缓存的思想，把ip组装成key存入缓存中，设置一个过期时间10s，那么10s内，该用户就无法再次参与抽奖。
   ![image](https://user-images.githubusercontent.com/38515687/122765226-e3b22480-d2d2-11eb-9b38-80548aaff296.png)



### 模拟post请求
![image](https://user-images.githubusercontent.com/38515687/122765441-16f4b380-d2d3-11eb-87cb-4f998817d6fc.png)

### 库存没有出现超卖
![image](https://user-images.githubusercontent.com/38515687/122765522-2a078380-d2d3-11eb-8e0e-486149bf0d85.png)

### 中奖纪录

完成情况
- [x] 合理的抽奖算法，保证概率均匀，不会因奖品抽走而导致概率变大

- [x] 将IP放入缓存中，设置过期时间，防止同一IP重复刷奖，增加中奖几率

- [x] 活动接口防刷，一般主键都是递增的，为了防止黑产利用这个特性刷奖，使用Uuid代替主键ID

- [x] 考虑限时抽奖可能造成瞬时高流量：采用Rhino对web抽奖接口进行限流，库存预先同步至Squirrel

- [x] 下游服务不可用时保证系统稳定性：Rhino熔断降级

- [x] Squirrel缓存与MySQL库存扣减的原子性

- [x] 采用分布式锁保证了库存不被超卖
  
- [x] 对数据库的扣减和落奖记录加事务，事务失败对缓存进行补偿，保证了缓存和DB的一致性

- [x] 使用数据库的唯一索引，保证幂等，不会使同一个ip重复中奖


