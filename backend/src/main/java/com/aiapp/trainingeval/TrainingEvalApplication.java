package com.aiapp.trainingeval;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 大学生软件实训教学 AI 检查评价系统 - 启动类
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.aiapp.trainingeval.mapper")
public class TrainingEvalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingEvalApplication.class, args);
    }
}
