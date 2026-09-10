package com.aiapp.trainingeval.config;

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
                        .title("实训 AI 评价系统 API")
                        .description("大学生软件实训教学 AI 检查评价系统接口文档")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0")));
    }
}
