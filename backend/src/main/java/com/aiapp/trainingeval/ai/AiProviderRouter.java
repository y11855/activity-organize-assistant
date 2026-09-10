package com.aiapp.trainingeval.ai;

import com.aiapp.trainingeval.ai.provider.OpenAiProvider;
import com.aiapp.trainingeval.ai.provider.OllamaProvider;
import com.aiapp.trainingeval.ai.provider.QwenProvider;
import com.aiapp.trainingeval.common.exception.BusinessException;
import com.aiapp.trainingeval.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 根据 application.yml 中 training-eval.ai.provider 动态选择实现
 */
@Component
public class AiProviderRouter {

    private final OpenAiProvider openAiProvider;
    private final OllamaProvider ollamaProvider;
    private final QwenProvider qwenProvider;

    @Value("${training-eval.ai.provider:openai}")
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
