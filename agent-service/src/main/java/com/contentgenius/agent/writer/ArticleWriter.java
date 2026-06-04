package com.contentgenius.agent.writer;

import com.contentgenius.agent.llm.LlmApiException;
import com.contentgenius.agent.llm.LlmErrorClassifier;
import com.contentgenius.agent.model.RouteType;
import com.contentgenius.agent.writer.assistant.ArticleWriterFallbackAssistant;
import com.contentgenius.agent.writer.assistant.ArticleWriterFastAssistant;
import com.contentgenius.agent.writer.assistant.ArticleWriterQualityAssistant;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 写稿门面：按路由档位委托给不同的 {@link dev.langchain4j.service.spring.AiService} 助手。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleWriter {

    private final ArticleWriterQualityAssistant qualityAssistant;
    private final ArticleWriterFallbackAssistant fallbackAssistant;
    private final ArticleWriterFastAssistant fastAssistant;

    public String write(RouteType routeType, String systemPrompt, String userPrompt) {
        if (routeType == RouteType.ARTICLE) {
            return qualityAssistant.write(systemPrompt, userPrompt);
        }
        if (routeType == RouteType.FAST) {
            return fastAssistant.write(systemPrompt, userPrompt);
        }
        if (routeType == RouteType.FALLBACK) {
            return fallbackAssistant.write(systemPrompt, userPrompt);
        }
        throw new IllegalArgumentException("未知 RouteType: " + routeType);
    }

    public TokenStream writeStream(RouteType routeType, String systemPrompt, String userPrompt) {
        if (routeType == RouteType.ARTICLE) {
            return qualityAssistant.writeStream(systemPrompt, userPrompt);
        }
        if (routeType == RouteType.FALLBACK) {
            return fallbackAssistant.writeStream(systemPrompt, userPrompt);
        }
        if (routeType == RouteType.FAST) {
            throw new IllegalArgumentException("FAST routeType is not supported for streaming");
        }
        throw new IllegalArgumentException("未知 RouteType: " + routeType);
    }

    /** 提纲：turbo 失败 → plus */
    public String outline(String systemPrompt, String userPrompt) {
        try {
            return fastAssistant.outline(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("outline", "主模型", ex);
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("outline 主模型失败，切换 fallback");
        }
        try {
            return fallbackAssistant.outline(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("outline", "备胎模型", ex);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    /** 润色：turbo 失败 → plus */
    public String style(String systemPrompt, String draft) {
        try {
            return fastAssistant.style(systemPrompt, draft);
        } catch (Exception ex) {
            logLlmFailure("style", "主模型", ex);
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("style 主模型失败，切换 fallback");
        }
        try {
            return fallbackAssistant.style(systemPrompt, draft);
        } catch (Exception ex) {
            logLlmFailure("style", "备胎模型", ex);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    /** 标题：turbo 失败 → plus */
    public String title(String systemPrompt, String content) {
        try {
            return fastAssistant.title(systemPrompt, content);
        } catch (Exception ex) {
            logLlmFailure("title", "主模型", ex);
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("title 主模型失败，切换 fallback");
        }
        try {
            return fallbackAssistant.title(systemPrompt, content);
        } catch (Exception ex) {
            logLlmFailure("title", "备胎模型", ex);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    /** 重写：max 失败 → plus */
    public String rewrite(String systemPrompt, String userPrompt) {
        try {
            return qualityAssistant.rewrite(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("rewrite", "主模型", ex);
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("rewrite 主模型失败，切换 fallback");
        }
        try {
            return fallbackAssistant.rewrite(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("rewrite", "备胎模型", ex);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    /** 长文正文：max 失败 → plus */
    public String writeArticleWithFallback(String systemPrompt, String userPrompt) {
        try {
            return qualityAssistant.write(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("write", "主模型", ex);
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("write 主模型失败，切换 fallback");
        }
        try {
            return fallbackAssistant.write(systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("write", "备胎模型", ex);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    private void logLlmFailure(String step, String stage, Throwable ex) {
        LlmApiException llm = LlmErrorClassifier.findLlmApiException(ex);
        if (llm != null) {
            log.warn("{} {}失败 HTTP {} code={} message={}",
                    step, stage, llm.getHttpStatus(), llm.getErrorCode(), llm.getErrorMessage());
        } else {
            log.warn("{} {}失败: {}", step, stage, ex.toString());
        }
    }
}
