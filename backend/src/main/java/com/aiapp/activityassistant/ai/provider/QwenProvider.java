package com.aiapp.activityassistant.ai.provider;

import com.aiapp.activityassistant.ai.AiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 阿里通义千问适配（DashScope 文本生成 API）
 * 文档：https://help.aliyun.com/zh/model-studio/developer-reference/use-qwen-by-calling-api
 */
@Slf4j
@Component
public class QwenProvider implements AiProvider {

    private static final String DASHSCOPE_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    @Value("${activity-assistant.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${activity-assistant.ai.qwen.model:qwen-plus}")
    private String model;

    @Value("${activity-assistant.ai.qwen.timeout:60000}")
    private long timeout;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置通义千问 API Key（环境变量 DASHSCOPE_API_KEY）");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "input", Map.of("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )),
                "parameters", Map.of("result_format", "message")
        );

        String response = webClient.post()
                .uri(DASHSCOPE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMillis(timeout));

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("output").path("choices").get(0)
                    .path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new RuntimeException("通义千问返回内容为空: " + response);
            }
            return content.asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析通义千问响应失败: " + response, e);
        }
    }

    @Override
    public String name() {
        return "qwen";
    }
}
