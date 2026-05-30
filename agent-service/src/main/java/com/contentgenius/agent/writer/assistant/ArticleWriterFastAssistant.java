package com.contentgenius.agent.writer.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 快速模型（qwen-turbo），供 RouteType.FAST 等短任务使用。
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenTurboChatModel")
public interface ArticleWriterFastAssistant {

    @SystemMessage("{{systemPrompt}}")
    String write(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );
}
