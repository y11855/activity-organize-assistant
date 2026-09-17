package com.aiapp.activityassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社团/班级活动组织 AI 助手 - 启动类
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.aiapp.activityassistant.mapper")
public class ActivityAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityAssistantApplication.class, args);
    }
}
