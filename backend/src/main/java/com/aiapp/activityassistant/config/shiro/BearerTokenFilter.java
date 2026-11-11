package com.aiapp.activityassistant.config.shiro;

import com.aiapp.activityassistant.common.context.UserContext;
import com.aiapp.activityassistant.common.result.Result;
import com.aiapp.activityassistant.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Bearer Token 认证过滤器：从 {@code Authorization} 头取出 token 交给 Shiro 认证，
 * 认证成功后填充 {@link UserContext}，请求结束时清理，避免 ThreadLocal 泄漏。
 *
 * <p>不使用 Shiro Session：每个请求凭 token 重新认证，保持完全无状态。</p>
 */
public class BearerTokenFilter extends AuthenticatingFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String token = extractToken((HttpServletRequest) request);
        return token == null ? null : new BearerAuthenticationToken(token);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        // 一律走 onAccessDenied → executeLogin，保证每次请求都用 token 重新认证
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        if (extractToken((HttpServletRequest) request) == null) {
            writeUnauthorized(response, ResultCode.UNAUTHORIZED.getMessage());
            return false;
        }
        return executeLogin(request, response);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject,
            ServletRequest request, ServletResponse response) {
        if (subject.getPrincipal() instanceof LoginUser loginUser) {
            UserContext.setCurrentUser(loginUser.getId(), loginUser.getRoles(),
                    loginUser.getPermissions().stream().toList());
        }
        // 继续执行后续过滤器与业务逻辑
        return true;
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e,
            ServletRequest request, ServletResponse response) {
        String message = e.getMessage() == null ? ResultCode.UNAUTHORIZED.getMessage() : e.getMessage();
        try {
            writeUnauthorized(response, message);
        } catch (IOException ex) {
            throw new IllegalStateException("写出认证失败响应时出错", ex);
        }
        return false;
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            super.doFilterInternal(request, response, chain);
        } finally {
            UserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(ServletResponse response, String message) throws IOException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                Result.fail(ResultCode.UNAUTHORIZED.getCode(), message)));
    }
}
