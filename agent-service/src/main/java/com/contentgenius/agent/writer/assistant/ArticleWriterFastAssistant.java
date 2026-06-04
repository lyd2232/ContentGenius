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

    @SystemMessage("{{systemPrompt}} 只输出文章大纲（分段标题+要点），不要写完整正文。")
    String outline(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );

    @SystemMessage("{{systemPrompt}} 保持事实与段落结构不变，按平台风格润色下文。")
    String style(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String draft
    );

    @SystemMessage("{{systemPrompt}} 根据下文生成1个标题，只输出标题一行。")
    String title(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String content
    );
}
