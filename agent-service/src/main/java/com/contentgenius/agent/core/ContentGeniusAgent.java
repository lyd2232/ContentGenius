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
import com.contentgenius.agent.tools.ChatAssistant;
import com.contentgenius.agent.writer.ArticleWriter;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
    // 联网摘要过长时做截断，避免 prompt 体积过大
    private static final int WEB_CONTEXT_MAX_LEN = 2500;
//查模板
    private final ContentTemplateClient contentTemplateClient;
    //存表
    private final Versions versionsClient;
    //构建提示词
    private final PromptBuilder promptBuilder;
    //写稿
    private final ArticleWriter articleWriter;
    //联网检索助手
    private final ChatAssistant chatAssistant;


    public AgentChatResponse chat(Long projectId, String topic, String platform) {
        // platform 为空时默认小红书，与 template 种子一致
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";

        // Nacos 命中时直接使用配置，避免多余查库与误告警
        String promptHint = resolvePromptHintByPriority(resolvedPlatform);
        // 联网检索主题上下文（失败时自动降级为 null）
        String webContext = loadWebContext(topic);

        // 构建stream和user提示
        String systemPrompt = promptBuilder.buildSystemPrompt(resolvedPlatform, promptHint, webContext);

        String userPrompt = promptBuilder.buildUserPrompt(topic);
        String authorization = currentAuthorizationHeader();

        //调大模型返回
        String content = generateWithFallback(systemPrompt, userPrompt);
//保存
        ContentVersionDto saved = saveVersion(projectId, topic, content, resolvedPlatform, authorization);
        return new AgentChatResponse(content, resolvedPlatform, saved.getId(), saved.getVersionNo());
    }
    /**
     * TokenStream 真流式：
     * <p>onPartialResponse 推 token，onCompleteResponse 落库并发 done，onError 处理 fallback。
     */
    public Flux<AgentChatResponse> streamChat(Long projectId, String topic, String platform) {
        // 1) 规范化平台参数：空值时用默认平台，避免后续 prompt/null 问题
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";

        // 2) Nacos 命中时直接使用配置，未命中再查模板表兜底
        String promptHint = resolvePromptHintByPriority(resolvedPlatform);
        // 2.1) 联网检索主题上下文
        String webContext = loadWebContext(topic);

        // 3) 组装 System 提示词（平台规则 + 模板 hint）
        String systemPrompt = promptBuilder.buildSystemPrompt(resolvedPlatform, promptHint, webContext);
        // 4) 组装 User 提示词（用户本次主题）
        String userPrompt = promptBuilder.buildUserPrompt(topic);
        // 5) 提前读取 Authorization（后续切线程后 RequestContext 可能拿不到）
        String authorization = currentAuthorizationHeader();
        // 6) 是否已输出过任意 token：控制主模型失败时能否“无缝切备胎”
        AtomicBoolean emittedPartial = new AtomicBoolean(false);
        // 7) 累计完整正文：每个 partial 都 append，最终落库用这个字符串
        StringBuilder fullContent = new StringBuilder();

        // 8) 创建 Flux：sink 是 SSE 出口，后续 next/complete/error 都写到这里
        return Flux.<AgentChatResponse>create(new Consumer<FluxSink<AgentChatResponse>>() {
                    @Override
                    public void accept(FluxSink<AgentChatResponse> sink) {
                        // 9) 进入 TokenStream 主逻辑（先主模型，必要时 fallback）
                        streamWithTokenStreamFallback(RouteType.ARTICLE, systemPrompt, userPrompt,
                                projectId, topic, resolvedPlatform, authorization, fullContent, emittedPartial, sink);
                    }
                })
                // 10) LLM 回调桥接 + Feign 存稿可能阻塞，放到弹性线程池执行
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void streamWithTokenStreamFallback(RouteType routeType,
                                               String systemPrompt,
                                               String userPrompt,
                                               Long projectId,
                                               String topic,
                                               String platform,
                                               String authorization,
                                               StringBuilder fullContent,
                                               AtomicBoolean emittedPartial,
                                               FluxSink<AgentChatResponse> sink) {
        // A) 由 RouteType 决定本轮调用哪个流式模型（ARTICLE=主模型，FALLBACK=备胎）
        TokenStream tokenStream = articleWriter.writeStream(routeType, systemPrompt, userPrompt);

        // B) 绑定 TokenStream 三类回调：partial / complete / error
        tokenStream
                .onPartialResponse(new Consumer<String>() {
                    @Override
                    public void accept(String partial) {
                        // B1) 客户端断开，或 token 为空：不再继续推送
                        if (sink.isCancelled() || partial == null || partial.isEmpty()) {
                            return;
                        }
                        // B2) 标记“已有输出”，用于错误时判断是否还能切备胎
                        emittedPartial.set(true);
                        // B3) 累计正文，供完成后落库
                        fullContent.append(partial);
                        // B4) 把本段 token 作为一条 SSE 消息推给前端
                        sink.next(new AgentChatResponse(partial, platform, null, null));
                    }
                })
                // C) 完成回调：模型正常输出结束后执行
                .onCompleteResponse(new Consumer<ChatResponse>() {
                    @Override
                    public void accept(ChatResponse response) {
                        // C1) 客户端已取消时，不再落库和收尾
                        if (sink.isCancelled()) {
                            return;
                        }
                        try {
                            // C2) 用完整正文落库 content_version
                            ContentVersionDto saved = saveVersion(projectId, topic, fullContent.toString(), platform, authorization);
                            // C3) 发送 done 事件：content 置空，仅携带版本信息
                            sink.next(new AgentChatResponse("", platform, saved.getId(), saved.getVersionNo()));
                            // C4) 正常结束 SSE 流
                            sink.complete();
                        } catch (Exception ex) {
                            // C5) 落库失败，向前端发错误终止
                            sink.error(ex);
                        }
                    }
                })
                // D) 错误回调：模型请求失败时执行
                .onError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable ex) {
                        // D1) 先打结构化日志，便于定位 HTTP 状态/错误码
                        logLlmFailure(routeType == RouteType.FALLBACK ? "备胎模型" : "主模型", ex);

                        // D2) 仅在主模型阶段考虑是否切备胎
                        if (routeType == RouteType.ARTICLE) {
                            // D3) 判断是否属于可降级错误（429/5xx/超时等）
                            boolean shouldFallback = LlmErrorClassifier.shouldFallback(ex);
                            // 主模型还没输出 token 时，允许切备胎无缝重试
                            if (shouldFallback && !emittedPartial.get()) {
                                // D4) 递归切换到备胎模型，复用同一 sink/fullContent
                                streamWithTokenStreamFallback(RouteType.FALLBACK, systemPrompt, userPrompt,
                                        projectId, topic, platform, authorization, fullContent, emittedPartial, sink);
                                return;
                            }
                            // D5) 不可降级错误：直接转换业务异常返回
                            if (!shouldFallback) {
                                sink.error(LlmErrorClassifier.toBusinessException(ex));
                                return;
                            }
                        }
                        // D6) 备胎失败或不可继续时，统一返回 503 业务异常
                        sink.error(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型暂时不可用，请稍后重试"));
                    }
                })
                // E) start() 必须调用：前面只是“注册回调”，这里才真正发起流式请求
                .start();
    }
//存稿内部方法
    private ContentVersionDto saveVersion(Long projectId, String topic, String content, String platform,
                                          String authorization) {
        VersionRequest request = new VersionRequest();
        //草稿标题
        request.setTitle(buildTitle(topic));
        request.setContent(content);
        request.setPlatform(platform);
        request.setSource(SOURCE_AGENT);

        try {
            //存稿失败
            Result<ContentVersionDto> result = versionsClient.create(projectId, authorization, request);
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

    /**
     * 从当前入站请求中读取 Authorization。
     * <p>流式链路切到 boundedElastic 后 ThreadLocal 上下文会丢失，所以要在入口线程先取出来并显式透传。
     */
    private static String currentAuthorizationHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getHeader("Authorization");
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

    private String resolvePromptHintByPriority(String platform) {
        if (promptBuilder.hasNacosPrompt(platform)) {
            log.info("命中 Nacos 平台提示词，跳过模板查询 platform={}", platform);
            return null;
        }
        return loadPromptHint(platform);
    }

    /**
     * 仅当 topic 明确需要联网时才检索；搜索成功后才扣 search 额度。
     */
    private String loadWebContext(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }
        // 普通写稿 topic 不触发联网，也不扣搜索额度
        if (!needsWebSearch(topic)) {
            log.debug("topic 未命中联网意图，跳过 WebSearch topic={}", topic);
            return null;
        }
        try {
            log.info("开始联网检索 topic={}", topic);
            String summary = chatAssistant.chat(topic.trim());
            if (!StringUtils.hasText(summary)) {
                log.warn("联网检索返回为空 topic={}", topic);
                return null;
            }
            String normalized = summary.trim();
            String webContext = normalized.length() <= WEB_CONTEXT_MAX_LEN
                    ? normalized
                    : normalized.substring(0, WEB_CONTEXT_MAX_LEN);
            log.info("联网检索成功 topic={} contextLength={}", topic, webContext.length());
            return webContext;
        } catch (BusinessException ex) {
            // 搜索额度用尽等业务异常必须抛出，不能静默降级
            throw ex;
        } catch (Exception ex) {
            log.warn("联网搜索失败，继续使用本地提示词 topic={}: {}", topic, ex.getMessage());
            return null;
        }
    }

    /** topic 含联网意图关键词时才走 Tavily */
    private static boolean needsWebSearch(String topic) {
        String t = topic.trim();
        return t.contains("搜") || t.contains("联网") || t.contains("网上")
                || t.contains("搜索") || t.contains("查一下") || t.contains("最新");
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
