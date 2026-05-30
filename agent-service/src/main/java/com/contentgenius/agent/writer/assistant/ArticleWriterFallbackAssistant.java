package com.contentgenius.agent.writer.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 主模型失败时的备胎（qwen-plus）。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenPlusChatModel")
public interface ArticleWriterFallbackAssistant {

    @SystemMessage("{{systemPrompt}}")
    String write(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );
}
