package com.aiapp.activityassistant.config;

import com.aiapp.activityassistant.config.shiro.BearerTokenFilter;
import com.aiapp.activityassistant.config.shiro.UserRealm;
import com.aiapp.activityassistant.service.RbacService;
import com.aiapp.activityassistant.service.TokenService;
import com.aiapp.activityassistant.service.UserService;
import jakarta.servlet.Filter;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 2.0 鉴权配置（适配 Spring Boot 3 / Jakarta EE 9+）。
 *
 * <p>认证：{@link BearerTokenFilter} 解析 Bearer token，交给 {@link UserRealm} 校验 Redis 会话，
 * 全程不创建 Shiro Session（无状态）。</p>
 * <p>授权：{@code @RequiresPermissions} 方法级鉴权，权限来自 RBAC 五表，
 * 数据级权限（活动创建者 / manage 权限）仍由业务层 checkAccess / checkManage 负责。</p>
 */
@Configuration
public class ShiroConfig {

    /**
     * Realm：token 校验 + 角色权限加载。关闭 Realm 自带缓存，
     * 权限缓存由 RbacService 的 Redis 实现（变更时主动失效），避免两层缓存不一致。
     */
    @Bean
    public UserRealm userRealm(TokenService tokenService, UserService userService, RbacService rbacService) {
        UserRealm realm = new UserRealm(tokenService, userService, rbacService);
        realm.setCachingEnabled(false);
        return realm;
    }

    @Bean
    public DefaultWebSecurityManager securityManager(UserRealm userRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);

        // 关键：禁用 Session 存储。项目是纯 token 无状态服务，
        // 若不关闭，Shiro 会为每个请求创建会话，用户量上来后内存持续增长。
        DefaultSessionStorageEvaluator evaluator = new DefaultSessionStorageEvaluator();
        evaluator.setSessionStorageEnabled(false);
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(evaluator);
        securityManager.setSubjectDAO(subjectDAO);
        return securityManager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean factory = new ShiroFilterFactoryBean();
        factory.setSecurityManager(securityManager);

        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("bearer", new BearerTokenFilter());
        factory.setFilters(filters);

        // 路径相对于 context-path（/api），无需带前缀；顺序敏感，具体路径在 /** 之前
        Map<String, String> chain = new LinkedHashMap<>();
        // 免登录接口
        chain.put("/auth/login", "anon");
        chain.put("/auth/register", "anon");
        chain.put("/auth/public-key", "anon");
        chain.put("/auth/captcha", "anon");
        chain.put("/auth/email-code", "anon");
        chain.put("/auth/login-by-email-code", "anon");
        chain.put("/auth/reset-code", "anon");
        chain.put("/auth/reset-password", "anon");
        // 上传文件静态映射
        chain.put("/uploads/**", "anon");
        // Knife4j / OpenAPI 文档
        chain.put("/doc.html", "anon");
        chain.put("/v3/api-docs/**", "anon");
        chain.put("/swagger-resources/**", "anon");
        chain.put("/webjars/**", "anon");
        chain.put("/favicon.ico", "anon");
        // Druid 监控页
        chain.put("/druid/**", "anon");
        // 错误页转发，避免二次认证
        chain.put("/error", "anon");
        // 其余全部需要 Bearer token，且禁止创建 Session
        chain.put("/**", "noSessionCreation,bearer");
        factory.setFilterChainDefinitionMap(chain);
        return factory;
    }

    /**
     * 启用 {@code @RequiresPermissions} 注解鉴权
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationAttributeSourceAdvisor.class)
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    /**
     * Controller 无接口，必须用 CGLIB 代理，否则注解鉴权静默失效
     */
    @Bean
    @ConditionalOnMissingBean(DefaultAdvisorAutoProxyCreator.class)
    public static DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }
}
