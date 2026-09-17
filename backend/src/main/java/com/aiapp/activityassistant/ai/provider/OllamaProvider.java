package com.aiapp.activityassistant.ai.provider;

import com.aiapp.activityassistant.ai.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 本地 Ollama 适配
 */
@Component
public class OllamaProvider implements AiProvider {

    @Value("${activity-assistant.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${activity-assistant.ai.ollama.model:qwen2.5:7b}")
    private String model;

    private final WebClient webClient = WebClient.create();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // TODO: POST {baseUrl}/api/chat
        // body: { model, stream: false, messages: [...] }
        throw new UnsupportedOperationException("待实现 Ollama 调用");
    }

    @Override
    public String name() {
        return "ollama";
    }
}
