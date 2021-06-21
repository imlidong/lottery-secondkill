package com.sankuai.mdp.lotterylidongservice.config;

import com.dianping.zebra.group.jdbc.GroupDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * @author lidong52
 * @version 1.0.0
 * @ClassName Config.java
 * @Description TODO
 * @createTime 2021年06月18日 18:28:00
 */
@Configuration
// 配置事务管理器
public class Config {
    @Bean(name = "txManager")
    public DataSourceTransactionManager dataSourceTransactionManager(GroupDataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
