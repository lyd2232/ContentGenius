package com.contentgenius.agent.writer.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 主模型失败时的备胎（qwen-plus）。
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenPlusChatModel",
        streamingChatModel = "qwenPlusStreamingChatModel")
public interface ArticleWriterFallbackAssistant {

    @SystemMessage("{{systemPrompt}}")
    String write(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );

    @SystemMessage("{{systemPrompt}}")
    TokenStream writeStream(
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

    @SystemMessage("{{systemPrompt}} 按用户要求重写下文，只输出完整正文。")
    String rewrite(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String userPrompt
    );
}
