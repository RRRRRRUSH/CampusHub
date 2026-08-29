package com.lnu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@MapperScan("com.lnu.mapper")
@EnableScheduling // 🟢 核心：开启定时任务支持
@SpringBootApplication
public class CampusServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusServerApplication.class, args);
    }

}
