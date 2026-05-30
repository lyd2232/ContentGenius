package com.contentgenius.agent.model;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 根据任务类型返回 LLMConfig 中注册的某一个 ChatModel Bean。
 */
@Component
public class ModelRouter {

    private final ChatModel qwenMaxChatModel;
    private final ChatModel qwenPlusChatModel;
    private final ChatModel qwenTurboChatModel;

    public ModelRouter(
            @Qualifier("qwenMaxChatModel") ChatModel qwenMaxChatModel,
            @Qualifier("qwenPlusChatModel") ChatModel qwenPlusChatModel,
            @Qualifier("qwenTurboChatModel") ChatModel qwenTurboChatModel) {
        this.qwenMaxChatModel = qwenMaxChatModel;
        this.qwenPlusChatModel = qwenPlusChatModel;
        this.qwenTurboChatModel = qwenTurboChatModel;
    }

    /**
     * @param type ARTICLE=写长文用 max，FAST=短任务用 turbo，FALLBACK=备胎 plus
     */
    public ChatModel resolve(RouteType type) {
        return switch (type) {
            case ARTICLE -> qwenMaxChatModel;
            case FAST -> qwenTurboChatModel;
            case FALLBACK -> qwenPlusChatModel;
        };
    }
}
