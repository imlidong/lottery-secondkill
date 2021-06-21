package com.sankuai.mdp.lotterylidongservice;

import com.meituan.swan.annotation.EnableSwan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import com.meituan.mdp.boot.starter.MdpContextUtils;

// @EnableSwan
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class ApplicationLoader {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ApplicationLoader.class);
        application.setAdditionalProfiles(MdpContextUtils.getHostEnvStr());
        application.run(args);
    }
}


