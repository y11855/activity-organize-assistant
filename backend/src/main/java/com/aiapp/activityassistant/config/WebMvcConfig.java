package com.aiapp.activityassistant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * Web MVC 配置：认证拦截器 + 上传文件静态映射
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 上传文件存储目录（Tomcat 部署时由环境变量 UPLOAD_DIR 指向绝对路径） */
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/public-key",
                        "/uploads/**",
                        // Knife4j / OpenAPI 文档
                        "/doc.html", "/v3/api-docs/**", "/swagger-resources/**",
                        "/webjars/**", "/favicon.ico",
                        // Druid 监控页
                        "/druid/**",
                        "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一转成绝对路径：外部 Tomcat 的工作目录是 bin/，相对路径会指错地方
        String dir = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir);
    }
}
