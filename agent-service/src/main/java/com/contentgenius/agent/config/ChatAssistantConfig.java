package com.contentgenius.agent.config;

import com.contentgenius.agent.tools.ChatAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchTool;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Configuration
public class ChatAssistantConfig {

    @Bean
    public ChatAssistant chatAssistant(@Qualifier("qwenTurboChatModel") ChatModel chatModel,
                                       WebSearchProperties props) {
        if (!StringUtils.hasText(props.getApiKey())) {
            log.warn("未配置 ai.models.web-search.api-key，联网搜索可能不可用");
        }
        // 封装为工具
        TavilyWebSearchEngine.TavilyWebSearchEngineBuilder builder = TavilyWebSearchEngine.builder()
                .apiKey(props.getApiKey())
                .timeout(Duration.ofSeconds(8));

        if (StringUtils.hasText(props.getEndpoint())) {
            builder.baseUrl(props.getEndpoint());
        }

        WebSearchTool webSearchTool = WebSearchTool.from(builder.build());
        // 绑定大模型
        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .tools(webSearchTool)
                .build();
    }
}
