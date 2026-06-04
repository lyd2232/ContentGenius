package com.contentgenius.agent.core;

import com.contentgenius.agent.model.ChatMode;
import lombok.Value;

/**
 * 一次写稿入口的公共上下文
 */
@Value
public class ChatContext {

    Long projectId;
    String topic;
    String platform;
    String systemPrompt;
    String userPrompt;
    String authorization;
    ChatMode mode;
    Integer memoryId;
}
