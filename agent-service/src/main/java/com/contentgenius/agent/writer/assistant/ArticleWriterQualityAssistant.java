package com.contentgenius.agent.writer.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 写长文主模型（qwen-max）的 AI 助手接口。
 * <p>启动时由 langchain4j-spring-boot-starter 扫描并生成实现类 Bean，无需手写 ChatRequest。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenMaxChatModel")
public interface ArticleWriterQualityAssistant {

    @SystemMessage("{{systemPrompt}}")
    String write(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );
}
