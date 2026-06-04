package com.contentgenius.agent.config;

import com.contentgenius.agent.llm.LlmHttpErrorInterceptor;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 注册三个 ChatModel Bean（qwen-max / plus / turbo），供 @AiService 注入。
 * <p>
 * HTTP 层使用 OkHttp，并挂载 {@link LlmHttpErrorInterceptor}，在非 2xx 时解析 JSON 错误。
 */
@Configuration
@EnableConfigurationProperties({
        QwenMaxProperties.class,
        QwenPlusProperties.class,
        QwenTurboProperties.class,
        PromptProperties.class,
        WebSearchProperties.class,
        QdrantProperties.class,
        EmbeddingProperties.class
})
public class LLMConfig {


    // TCP 建连最长等待时间（与 Feign 的 connectTimeout 同理，但只作用于调 DashScope）
    private static final Duration LLM_CONNECT_TIMEOUT = Duration.ofSeconds(15);

    // 等待大模型生成完响应的最长时间（写稿可能较慢）
    private static final Duration LLM_READ_TIMEOUT = Duration.ofSeconds(120);

    /** 写长文主模型，Bean 名与 @AiService(chatModel = "qwenMaxChatModel") 一致 */
    @Bean("qwenMaxChatModel")
    public ChatModel qwenMaxChatModel(QwenMaxProperties props) {
        return build(props.getEndpoint(), props.getApiKey(), props.getModelName(),
                props.getTemperature(), props.getMaxTokens());
    }

    /** 写长文主模型的流式版本，供 SSE 接口使用 */
    @Bean("qwenMaxStreamingChatModel")
    public StreamingChatModel qwenMaxStreamingChatModel(QwenMaxProperties props) {
        return buildStreaming(props.getEndpoint(), props.getApiKey(), props.getModelName(),
                props.getTemperature(), props.getMaxTokens());
    }

    /** 备胎模型，主模型 429/5xx 时由 ContentGeniusAgent 切换使用 */
    @Bean("qwenPlusChatModel")
    public ChatModel qwenPlusChatModel(QwenPlusProperties props) {
        return build(props.getEndpoint(), props.getApiKey(), props.getModelName(),
                props.getTemperature(), props.getMaxTokens());
    }

    /** 备胎模型的流式版本 */
    @Bean("qwenPlusStreamingChatModel")
    public StreamingChatModel qwenPlusStreamingChatModel(QwenPlusProperties props) {
        return buildStreaming(props.getEndpoint(), props.getApiKey(), props.getModelName(),
                props.getTemperature(), props.getMaxTokens());
    }

    /** 快速模型，RouteType.FAST 预留 */
    @Bean("qwenTurboChatModel")
    public ChatModel qwenTurboChatModel(QwenTurboProperties props) {
        return build(props.getEndpoint(), props.getApiKey(), props.getModelName(),
                props.getTemperature(), props.getMaxTokens());
    }

    /**
     * 三个 Bean 共用的构建逻辑。
     */
    private static ChatModel build(String endpoint, String apiKey, String modelName,
                                   Double temperature, Integer maxTokens) {
        // 原生 OkHttp 配置：超时 + 错误拦截器（读 HTTP 状态与 JSON）
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .connectTimeout(LLM_CONNECT_TIMEOUT)
                .readTimeout(LLM_READ_TIMEOUT)
                // 非 2xx 时抛 LlmApiException，不再只靠 LangChain4j 封装的模糊文案
                .addInterceptor(new LlmHttpErrorInterceptor());

        // 交给 LangChain4j 的 OkHttp 适配层
        OkHttpClientBuilder httpClientBuilder = new OkHttpClientBuilder()
                .okHttpClientBuilder(okHttpBuilder);

        return OpenAiChatModel.builder()
                // 指定自定义 HTTP 客户端（含上面的拦截器）
                .httpClientBuilder(httpClientBuilder)
                // DashScope
                .baseUrl(endpoint)
                // 会自动写入 Authorization: Bearer <apiKey>
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature != null ? temperature : 0.5)
                .maxTokens(maxTokens)
                // LangChain4j 层面的总超时，与 OkHttp readTimeout 对齐
                .timeout(LLM_READ_TIMEOUT)
                // 关闭 LangChain4j 内置同模型重试，避免与「max → plus」fallback 重复
                .maxRetries(0)
                .build();
    }

    /**
     * SSE 接口使用的流式模型构建逻辑。
     */
    private static StreamingChatModel buildStreaming(String endpoint, String apiKey, String modelName,
                                                     Double temperature, Integer maxTokens) {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .connectTimeout(LLM_CONNECT_TIMEOUT)
                .readTimeout(LLM_READ_TIMEOUT)
                .addInterceptor(new LlmHttpErrorInterceptor());

        OkHttpClientBuilder httpClientBuilder = new OkHttpClientBuilder()
                .okHttpClientBuilder(okHttpBuilder);

        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .baseUrl(endpoint)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature != null ? temperature : 0.5)
                .maxTokens(maxTokens)
                .timeout(LLM_READ_TIMEOUT)
                .build();
    }
    /**
     * 会话记忆（进程内）；{@link com.contentgenius.agent.writer.assistant.ReviseChatAssistant} 通过 {@code @MemoryId} 使用。
     * 后续可改为 {@code .chatMemoryStore(redisChatMemoryStore)} 做 Redis 持久化。
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(5) // 保留最近5轮对话
                .build();
    }
}
