package com.aiapp.activityassistant.config;

import com.aiapp.activityassistant.common.mq.ActivityEventPublisher;
import com.aiapp.activityassistant.common.mq.UserBehaviorPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 主题声明：启动时自动创建事件主题。
 * 3 分区按 ownerId 散列，保证同一用户的大屏动态有序；单副本适配单机开发环境。
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic activityEventsTopic() {
        return TopicBuilder.name(ActivityEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 用户行为主题：Flink 作业消费后聚合兴趣得分，分区数与事件主题保持一致
     */
    @Bean
    public NewTopic userBehaviorTopic() {
        return TopicBuilder.name(UserBehaviorPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
