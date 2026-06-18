package com.oceanverse;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * OceanVerse — 智慧海洋探索平台
 * 启动类
 */
@Slf4j
@SpringBootApplication
@MapperScan(basePackages = {
        "com.oceanverse.auth.mapper",
        "com.oceanverse.species.mapper",
        "com.oceanverse.eco.mapper",
        "com.oceanverse.ai.mapper",
        "com.oceanverse.community.mapper",
        "com.oceanverse.visual.mapper",
        "com.oceanverse.message.mapper"
})
public class OceanVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(OceanVerseApplication.class, args);
        log.info("============================================");
        log.info("OceanVerse — 智慧海洋探索平台 启动成功！");
        log.info("API 文档: http://localhost:8080/swagger-ui.html");
        log.info("============================================");
    }
}
