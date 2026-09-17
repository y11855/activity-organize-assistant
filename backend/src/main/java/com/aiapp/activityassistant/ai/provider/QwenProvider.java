package com.aiapp.activityassistant.ai.provider;

import com.aiapp.activityassistant.ai.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里通义千问适配（DashScope）
 */
@Component
public class QwenProvider implements AiProvider {

    @Value("${activity-assistant.ai.qwen.api-key:}")
    private String apiKey;

    @Value("${activity-assistant.ai.qwen.model:qwen-plus}")
    private String model;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // TODO: 调用 https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
        throw new UnsupportedOperationException("待实现通义千问调用");
    }

    @Override
    public String name() {
        return "qwen";
    }
}
