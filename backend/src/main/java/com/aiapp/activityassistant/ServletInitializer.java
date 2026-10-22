package com.aiapp.activityassistant;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 外部 Tomcat 部署入口。
 * <p>
 * 打包成 war 交给外部 Tomcat（10.1 / Jakarta EE）时，Tomcat 通过 SPI 找到本类来启动 Spring 容器；
 * 以 java -jar 或 mvn spring-boot:run 方式运行时不会用到它。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ActivityAssistantApplication.class);
    }
}
