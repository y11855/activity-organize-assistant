package com.aiapp.activityassistant.config;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * JSON 请求体缓存过滤器。
 * <p>请求体在 Servlet 流中只能读一次，而幂等拦截器需要先读请求体算摘要、
 * 控制器随后还要反序列化，因此在这里提前读出字节并包装成可重复读取的请求。</p>
 * <p>只处理 application/json，文件上传等大请求体不参与缓存。</p>
 */
@Component
public class CachedBodyFilter extends OncePerRequestFilter {

    /** 请求属性：JSON 请求体的 SHA-256 摘要，供 {@link IdempotentInterceptor} 使用 */
    public static final String BODY_DIGEST_ATTRIBUTE = "IDEMPOTENT_BODY_DIGEST";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readAllBytes();
        request.setAttribute(BODY_DIGEST_ATTRIBUTE, DigestUtil.sha256Hex(body));
        chain.doFilter(new CachedBodyRequestWrapper(request, body), response);
    }

    /** 用缓存字节数组重建输入流，保证 @RequestBody 仍能正常读取 */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // 同步读取，无需回调
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
