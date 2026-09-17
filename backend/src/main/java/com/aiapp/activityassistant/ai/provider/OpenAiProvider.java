package com.aiapp.activityassistant.ai.provider;

import com.aiapp.activityassistant.ai.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI 适配
 */
@Component
public class OpenAiProvider implements AiProvider {

    @Value("${activity-assistant.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${activity-assistant.ai.openai.api-key:}")
    private String apiKey;

    @Value("${activity-assistant.ai.openai.model:gpt-4o-mini}")
    private String model;

    private final WebClient webClient = WebClient.create();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // TODO: 调用 OpenAI Chat Completions API
        throw new UnsupportedOperationException("待实现 OpenAI 调用");
    }

    @Override
    public String name() {
        return "openai";
    }
}
