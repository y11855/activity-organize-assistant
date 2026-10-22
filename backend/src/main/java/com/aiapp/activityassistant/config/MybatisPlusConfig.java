package com.aiapp.activityassistant.config;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 主键生成器配置。
 *
 * <p>实体主键使用 {@code IdType.ASSIGN_ID}（雪花 ID）。MyBatis-Plus 默认生成器会根据机器地址
 * 推导 workerId —— 在同一台机器上跑多个后端实例（单机集群）时会算出一致的 workerId，从而产生
 * 重复 ID。这里改为通过环境变量 {@code WORKER_ID} 显式区分每个实例（例如 backend1 注入 1、
 * backend2 注入 2）。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /** 雪花 ID 的 workerId，由每个实例通过环境变量注入，默认 1。 */
    @Value("${WORKER_ID:1}")
    private long workerId;

    @Bean
    public IdentifierGenerator identifierGenerator() {
        // datacenterId 固定为 1；workerId 由实例区分，避免同机多实例撞号
        return new DefaultIdentifierGenerator(workerId, 1L);
    }
}
