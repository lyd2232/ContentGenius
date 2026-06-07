package com.contentgenius.agent.core;

import com.contentgenius.agent.model.ChatMode;
import lombok.Value;

/**
 * 一次写稿入口的公共上下文
 */
@Value
public class ChatContext {

    Long projectId;
    /** 左侧创作主题，与本轮指令物理隔离 */
    String creationTheme;
    /** 本轮指令（首版/改稿要求） */
    String topic;
    String platform;
    String systemPrompt;
    String userPrompt;
    String authorization;
    ChatMode mode;
    Integer memoryId;
    /** 思考模式显式动作：title | outline | rewrite | style */
    String thinkAction;
    /** 入口线程捕获的登录用户 id，异步/流式回调不得再读 SecurityContext */
    Long userId;
    /** 联网检索摘要（仅注入 prompt；成稿时可追加参考链接） */
    String webContext;
}
