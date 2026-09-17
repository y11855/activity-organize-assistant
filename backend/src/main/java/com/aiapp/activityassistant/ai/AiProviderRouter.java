package com.aiapp.activityassistant.ai;

import com.aiapp.activityassistant.ai.provider.OpenAiProvider;
import com.aiapp.activityassistant.ai.provider.OllamaProvider;
import com.aiapp.activityassistant.ai.provider.QwenProvider;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 根据 application.yml 中 activity-assistant.ai.provider 动态选择实现
 */
@Component
public class AiProviderRouter {

    private final OpenAiProvider openAiProvider;
    private final OllamaProvider ollamaProvider;
    private final QwenProvider qwenProvider;

    @Value("${activity-assistant.ai.provider:openai}")
    private String provider;

    public AiProviderRouter(OpenAiProvider openAiProvider,
                            OllamaProvider ollamaProvider,
                            QwenProvider qwenProvider) {
        this.openAiProvider = openAiProvider;
        this.ollamaProvider = ollamaProvider;
        this.qwenProvider = qwenProvider;
    }

    public AiProvider choose() {
        return switch (provider.toLowerCase()) {
            case "ollama" -> ollamaProvider;
            case "qwen" -> qwenProvider;
            case "openai" -> openAiProvider;
            default -> throw new BusinessException(ResultCode.AI_CALL_ERROR, "未知 AI provider: " + provider);
        };
    }
}
