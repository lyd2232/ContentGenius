package com.contentgenius.agent.core;

import com.contentgenius.agent.client.ContentTemplateClient;
import com.contentgenius.agent.client.Versions;
import com.contentgenius.agent.config.PromptBuilder;
import com.contentgenius.agent.dto.AgentChatResponse;
import com.contentgenius.agent.dto.ContentVersionDto;
import com.contentgenius.agent.dto.TemplateDto;
import com.contentgenius.agent.dto.VersionRequest;
import com.contentgenius.agent.llm.LlmApiException;
import com.contentgenius.agent.llm.LlmErrorClassifier;
import com.contentgenius.agent.model.RouteType;
import com.contentgenius.agent.writer.ArticleWriter;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 写稿编排核心：拉 template → 拼 Prompt → 调 @AiService 写稿 → Feign 存 content_version。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentGeniusAgent {
//存表agent写出来的
    private static final String SOURCE_AGENT = "agent";
    //草稿标题最大长度
    private static final int TITLE_MAX_LEN = 256;
//查模板
    private final ContentTemplateClient contentTemplateClient;
    //存表
    private final Versions versionsClient;
    //构建提示词
    private final PromptBuilder promptBuilder;
    //写稿
    private final ArticleWriter articleWriter;

    public AgentChatResponse chat(Long projectId, String topic, String platform) {
        // platform 为空时默认小红书，与 template 种子一致
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";

        // 查content中的模板
        String promptHint = loadPromptHint(resolvedPlatform);

        // 构建stream和user提示
        String systemPrompt = promptBuilder.buildSystemPrompt(resolvedPlatform, promptHint, null);
        String userPrompt = promptBuilder.buildUserPrompt(topic);

        //调大模型返回
        String content = generateWithFallback(systemPrompt, userPrompt);
//保存
        ContentVersionDto saved = saveVersion(projectId, topic, content, resolvedPlatform);
        return new AgentChatResponse(content, resolvedPlatform, saved.getId(), saved.getVersionNo());
    }
//存稿内部方法
    private ContentVersionDto saveVersion(Long projectId, String topic, String content, String platform) {
        VersionRequest request = new VersionRequest();
        //草稿标题
        request.setTitle(buildTitle(topic));
        request.setContent(content);
        request.setPlatform(platform);
        request.setSource(SOURCE_AGENT);

        try {
            //存稿失败
            Result<ContentVersionDto> result = versionsClient.create(projectId, request);
            if (result == null || result.getData() == null) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "存稿失败：content-service 无有效响应");
            }
            log.info("AI 草稿已存入 content_version projectId={} versionId={} versionNo={}",
                    projectId, result.getData().getId(), result.getData().getVersionNo());
            return result.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Feign 存版本失败 projectId={}: {}", projectId, ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "存稿失败，请稍后重试");
        }
    }
//检查草稿
    private static String buildTitle(String topic) {
        if (!StringUtils.hasText(topic)) {
            return "AI 草稿";
        }
        String trimmed = topic.trim();
        return trimmed.length() <= TITLE_MAX_LEN ? trimmed : trimmed.substring(0, TITLE_MAX_LEN);
    }

    private String loadPromptHint(String platform) {
        try {
            Result<List<TemplateDto>> result = contentTemplateClient.listTemplates(platform);
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                log.warn("未查到平台模板 platform={}，使用 PromptBuilder 内置默认", platform);
                return null;
            }
            TemplateDto template = result.getData().get(0);
            return template.getPromptHint();
        } catch (Exception ex) {
            log.warn("拉取 content 模板失败 platform={}，使用内置默认: {}", platform, ex.getMessage());
            return null;
        }
    }

    /**
     * 大模型协调关键点
     */
    private String generateWithFallback(String systemPrompt, String userPrompt) {
        try {
            //默认使用主模型写
            return articleWriter.write(RouteType.ARTICLE, systemPrompt, userPrompt);
        } catch (Exception ex) {
            // 出现异常，抛出异常
            logLlmFailure("主模型", ex);

            //如果不在可重写的shouldFallback错误，则抛出toBusinessException业务异常
            if (!LlmErrorClassifier.shouldFallback(ex)) {
                throw LlmErrorClassifier.toBusinessException(ex);
            }
            log.warn("主模型失败，按 HTTP/网络规则切换 fallback 模型");
        }

        try {
            //使用备胎模型
            return articleWriter.write(RouteType.FALLBACK, systemPrompt, userPrompt);
        } catch (Exception ex) {
            logLlmFailure("备胎模型", ex);
            // 备胎也不行直接报错
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试");
        }
    }

    /**
     * 记录大模型失败详情；若有 {@link LlmApiException} 则打印 HTTP 与 JSON 字段。
     */
    private void logLlmFailure(String stage, Throwable ex) {
        LlmApiException llm = LlmErrorClassifier.findLlmApiException(ex);
        if (llm != null) {
            log.warn("{}失败 HTTP {} code={} type={} message={} rawBody={}",
                    stage, llm.getHttpStatus(), llm.getErrorCode(), llm.getErrorType(),
                    llm.getErrorMessage(), truncate(llm.getRawBody(), 300));
        } else {
            // 纯网络超时等，没有 HTTP 响应体
            log.warn("{}失败: {}", stage, ex.getMessage());
        }
    }

    /** 日志里 body 过长时截断，防止刷屏 */
    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
