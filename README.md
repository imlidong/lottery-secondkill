# lottery-secondkill
总体要求

设计一个抽奖系统，提供抽奖功能。

•  Web服务：

   • 选择具体的抽奖活动进行抽奖。

•  Thrift服务：

   • 选择具体的抽奖活动进行抽奖。

•  抽奖活动及奖品信息和奖品库存的创建可以采用手动录入数据库的方式创建。

技术细节

聚合根：抽奖活动-LotteryActivity（活动编号，上线时间，下线时间，奖品列表，活动状态）

实体：奖品-Prize（活动编号，奖品id，几等奖，中奖概率，库存，物品）

实体：物品-goods（物品编号，物品名称，物品价格，物品图片）

实体：中奖纪录-WinningRecord（活动id，奖品id，物品id，IP，抽奖时间）

实体类设计

一、抽奖算法的设计实现

算法思想

给每种奖品都有一个权重，对应一个区间，若落入该区间就表示中奖，那么通过动态调整区间大小就可改变获奖概率，即调整权重值即可

然后通过random一个随机数，来判断落入了哪个区间即可。

randNum = new Random().nextInt(totalWeight); // totalWeight = 上面权重列之和

randNum = 8944 落在未中奖区间 未中奖 
randNum = 944 落在1元区间 中了一元

总结：这个抽奖算法其实就是通过调整各个奖项的权重来调整中奖的概率，因为是考虑瞬时高流量的抽奖，所以还涉及库存问题，因此即是否中奖除了落入区间外，还需判断减库存是否成功，如果减库存失败 仍当做未中奖，关于库存扣减见下面内容。

二、数据库表

TB_Goods物品表

create table if not exists TB_Goods
(
	ID int auto_increment comment '主键ID'
		primary key,
	GoodsName varchar(64) not null comment '物品名称',
	Price int not null comment '物品价值',
	ImageList varchar(128) not null comment '图片列表'
)
comment '物品信息表' charset=utf8;



抽奖活动表

create table TB_LotteryActivity
(
	ID int auto_increment comment '活动主键ID'
		primary key,
	ActiveStatus tinyint not null comment '活动状态',
	StartTime datetime not null comment '开始时间',
	EndTime datetime not null comment '结束时间',
	Uuid varchar(64) null,
	constraint TB_LotteryActivity_Uuid_uindex
		unique (Uuid)
)
comment '抽奖活动设置纪录表' charset=utf8;
create index UX_EndTime
	on TB_LotteryActivity (EndTime);
create index UX_StartTime
	on TB_LotteryActivity (StartTime);


奖品表

CREATE TABLE `TB_Prize` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ActivityID` int(11) NOT NULL COMMENT '对应的活动ID',
  `GoodsID` int(11) NOT NULL COMMENT '对应的物品ID',
  `PrizeLevel` int(11) NOT NULL COMMENT '几等奖',
  `Probability` int(11) NOT NULL COMMENT '中奖概率，代码逻辑中除以10000',
  `Stock` int(11) NOT NULL COMMENT '库存量',
  PRIMARY KEY (`ID`),
  KEY `IDX_ActivityID` (`ActivityID`),
  KEY `IDX_GoodsID` (`GoodsID`),
  KEY `IDX_Stock` (`Stock`),
  KEY `IDX_Probability` (`Probability`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 COMMENT='奖品信息表'

中奖纪录表

CREATE TABLE `TB_WinningRecord` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ActivityID` int(11) NOT NULL COMMENT '活动ID',
  `PrizeID` int(11) NOT NULL COMMENT '奖品ID',
  `GoodsID` int(11) NOT NULL COMMENT '物品ID',
  `GoodsName` varchar(64) NOT NULL COMMENT '物品名称',
  `IP` varchar(64) NOT NULL COMMENT '中奖ip地址',
  `LotteryTime` datetime NOT NULL COMMENT '中奖时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `TB_WinningRecord_IP_uindex` (`IP`),
  KEY `TB_WinningRecord_ActivityID_index` (`ActivityID`)
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8 COMMENT='中奖记录表'

三、系统核心逻辑

扣减逻辑整体描述

考虑到瞬时抽奖的流量是非常高的，因此对于库存系统来说是个很大的考验。系统主要使用了squirrel缓存，mafka消息队列中间件等避免大量请求直接落到db，进而导致崩溃。因此，我们首先在活动开始前，将db的库存全量同步都缓存当中，先扣减缓存，再异步通知db扣减。

以下是一个请求到来后的逻辑（假设已通过抽奖算法落入某个奖项的区间了）

IP限制

为了防止同一个IP地址连续重复的发起请求，增加中奖概率，需要对IP进行限制，主要采用缓存的思想，把ip组装成key存入缓存中，设置一个过期时间10s，那么10s内，该用户就无法再次参与抽奖。

活动时间校验

时间节点可以缓存起来，这样不用每次都走db查，不然肯定奔溃。运营那边修改了时间后，可以再更新缓存。逻辑上，如果没到时间，返回活动未开始；如果超过时间，返回活动已结束。

          

缓存扣减

首先从缓存中通过奖品ID获取到某个奖品的库存，如果库存小于等于0了（无库存），返回没有足够库存的响应。如果奖品库存大于0（库存存在），那么该线程通过自旋的方式去获取分布式锁（最多自旋5次，防止cpu做无用消耗），如果没有获取到锁，直接返回不中奖；如果获取到锁，再次做库存的校验（二次检查，因为中间可能被人修改了），库存仍旧足够的情况下，进行缓存的原子扣减，然后通过mafka异步的给db发送一条扣减消息。

     

DB扣减

mafka监听到扣减消息之后，要执行库存的扣减。

但假如库存只剩1个了，有10个用户同时落入一元区间，如何避免1-10=-9的情况呢？

库存扣减使用以下sql进行扣减（也可以使用数据库的乐观锁）

<update id="deductStock" parameterType="java.util.Map"> update TB_Prize set `Stock` = `Stock`-1 where `ID`= #{id} and <![CDATA[ `Stock`> 0]]></update>

同时，在扣减完库存之后，要落一个中奖纪录到另外一个表中，只有扣库存和记录落表之后，才算是真正的中奖了。

<insert id="insert" parameterType="java.util.Map"> insert into TB_WinningRecord (`ID`, `ActivityID`, `PrizeID`,`GoodsID`,`GoodsName`,`IP`,`LotteryTime`) values (#{id},#{activityId},#{prizeId},#{goodsId},#{goodsName},#{ip},NOW())</insert>

DB扣减失败的情况

db有可能出现扣减失败的情况，这样子就不能落中奖纪录，因此需要把扣db库存和落中奖纪录两个操作放到一个事务当中。如果出现失败，那么进行事务的回滚。同时，因为失败了，缓存也需要更新，这个时候通过缓存的原子加操作进行对应缓存库存的回滚补偿，保证缓存和db的一致性

防遍历刷接口

给活动字段增加Uuid，传参改为用Uuid的形式，防止被黑产刷接口

如何防止同一个IP用户重复中奖

这里用了简单的思想，使用数据库ip字段建立唯一索引做了幂等，如果已经有中奖纪录，那么会返回失败，这样就触发了事务回滚，也不会再中奖成功。

下游服务突然挂了，如何保证服务稳定性

使用了Rhino进行降级熔断

使用Rhino的统一接口进行限流

模拟post请求

库存没有出现超卖情况

中奖纪录

完成情况

合理的抽奖算法，保证概率均匀，不会因奖品抽走而导致概率变大

将IP放入缓存中，设置过期时间，防止同一IP重复刷奖，增加中奖几率

活动接口防刷，一般主键都是递增的，为了防止黑产利用这个特性刷奖，使用Uuid代替主键ID

考虑限时抽奖可能造成瞬时高流量：采用Rhino对web抽奖接口进行限流，库存预先同步至Squirrel

下游服务不可用时保证系统稳定性：Rhino熔断降级

Squirrel缓存与MySQL库存扣减的原子性

采用分布式锁保证了库存不被超卖

对数据库的扣减和落奖记录加事务，事务失败对缓存进行补偿，保证了缓存和DB的一致性

使用数据库的唯一索引，保证幂等，不会使同一个ip重复中奖
