package com.aiapp.activityassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 全局配置：Long 序列化为字符串，避免雪花 ID 在前端 JS number 中精度丢失
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }

    /**
     * HTTP 响应主 ObjectMapper（@Primary）。
     * 必须显式声明：本类中的 draftObjectMapper 同样是 ObjectMapper 类型的 Bean，
     * 会满足 Spring Boot 自动配置的 @ConditionalOnMissingBean 条件、导致默认主 mapper 不再创建，
     * 从而使 HTTP 响应退化为「雪花 ID 以数字返回」，前端 JS 大整数精度丢失 → 请求 id 失真。
     * 这里基于自动配置的 Jackson2ObjectMapperBuilder（已应用上面的自定义定制）构建主 mapper。
     */
    @Bean
    @Primary
    public ObjectMapper primaryObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false).build();
    }

    /**
     * 活动草稿专用 ObjectMapper：支持 LocalDateTime 等 Java8 时间类型，统一输出 ISO 字符串。
     * 草稿 JSON 落库/读取共用同一实例，避免各处重复配置。
     * 不启用 Long→String 定制（草稿仅在后端 Java 内序列化/反序列化，无 JS 精度问题）。
     */
    @Bean("draftObjectMapper")
    public ObjectMapper draftObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
