package com.aiapp.activityassistant.ai;

import java.util.List;

/**
 * 大模型统一接口
 */
public interface AiProvider {

    /**
     * 对话调用
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式对话
     */
    default String streamChat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt);
    }

    /**
     * 多轮对话
     */
    default String chat(List<AiMessage> messages) {
        throw new UnsupportedOperationException();
    }

    /**
     * Provider 名称
     */
    String name();
}
