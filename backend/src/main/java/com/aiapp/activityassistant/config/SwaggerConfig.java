package com.aiapp.activityassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档配置 (Knife4j / OpenAPI 3)
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("活动管家 API")
                        .description("社团/班级活动组织 AI 助手接口文档")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0")));
    }
}
