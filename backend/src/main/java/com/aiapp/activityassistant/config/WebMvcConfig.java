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
 * Web MVC 配置：幂等拦截器 + 上传文件静态映射。
 * 认证与免登录白名单已迁移到 Shiro Filter，见 {@link ShiroConfig}。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IdempotentInterceptor idempotentInterceptor;

    /** 上传文件存储目录（Tomcat 部署时由环境变量 UPLOAD_DIR 指向绝对路径） */
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Shiro 过滤器在 Servlet 层先完成认证并填充 UserContext，这里保持幂等拦截在其之后
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/**");
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
