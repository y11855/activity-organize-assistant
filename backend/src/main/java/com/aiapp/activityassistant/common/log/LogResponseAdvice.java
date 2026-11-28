package com.aiapp.activityassistant.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.InputStream;

/**
 * 响应体采集：把接口返回的 JSON 截断后暂存到请求属性中。
 *
 * <p>日志拦截器的 afterCompletion 阶段拿不到响应体，这里在序列化前顺带保存一份，
 * 供日志中心展开行展示「响应内容」。文件流、二进制内容不采集。</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class LogResponseAdvice implements ResponseBodyAdvice<Object> {

    /** 请求属性：响应体 JSON，供 {@link LogInterceptor} 读取 */
    public static final String RESPONSE_BODY_ATTRIBUTE = "SYS_LOG_RESPONSE_BODY";

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 只处理可序列化的普通对象；文件下载、字节流直接跳过
        if (body == null || body instanceof byte[] || body instanceof Resource || body instanceof InputStream) {
            return body;
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            try {
                httpRequest.setAttribute(RESPONSE_BODY_ATTRIBUTE,
                        LogTextUtils.truncate(objectMapper.writeValueAsString(body)));
            } catch (Exception ignored) {
                // 采集失败不影响正常响应
            }
        }
        return body;
    }
}
