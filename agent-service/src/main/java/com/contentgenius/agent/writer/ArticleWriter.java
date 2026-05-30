package com.contentgenius.agent.writer;

import com.contentgenius.agent.model.RouteType;
import com.contentgenius.agent.writer.assistant.ArticleWriterFallbackAssistant;
import com.contentgenius.agent.writer.assistant.ArticleWriterFastAssistant;
import com.contentgenius.agent.writer.assistant.ArticleWriterQualityAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 写稿门面：按路由档位委托给不同的 {@link dev.langchain4j.service.spring.AiService} 助手。
 * <p>底层 ChatModel 仍由 {@link com.contentgenius.agent.config.LLMConfig} 注册，助手由 Spring 自动生成代理。
 */
@Component
@RequiredArgsConstructor
public class ArticleWriter {

    private final ArticleWriterQualityAssistant qualityAssistant;
    private final ArticleWriterFallbackAssistant fallbackAssistant;
    private final ArticleWriterFastAssistant fastAssistant;

    /**
     * @param routeType    ARTICLE / FAST / FALLBACK
     * @param systemPrompt 平台风格 + prompt_hint（来自 PromptBuilder）
     * @param userPrompt   用户主题
     */
    public String write(RouteType routeType, String systemPrompt, String userPrompt) {
        return switch (routeType) {
            case ARTICLE -> qualityAssistant.write(systemPrompt, userPrompt);
            case FAST -> fastAssistant.write(systemPrompt, userPrompt);
            case FALLBACK -> fallbackAssistant.write(systemPrompt, userPrompt);
        };
    }
}
