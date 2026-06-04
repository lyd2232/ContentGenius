package com.contentgenius.agent.writer.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 多轮改稿专用：使用 {@link MemoryId} 的写稿接口。
 * <p>
 * 大纲 / 正文 / 润色 / 标题 流水线（{@link ArticleWriterFastAssistant}、
 * {@link ArticleWriterQualityAssistant}）<b>不要</b>加 {@link MemoryId}，同一次请求内用参数串联即可。
 * <p>
 * 记忆仅存「用户指令 + 完整成稿」；当前为进程内窗口记忆，后续再接 RedisChatMemoryStore。
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenMaxChatModel",
        chatMemoryProvider = "chatMemoryProvider")
public interface ReviseChatAssistant {

    @SystemMessage("{{systemPrompt}}")
    String chat(
            @MemoryId String sessionId,
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userMessage
    );
}
