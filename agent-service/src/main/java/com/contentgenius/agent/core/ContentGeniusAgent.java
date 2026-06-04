package com.contentgenius.agent.core;

import com.contentgenius.agent.client.ContentTemplateClient;
import com.contentgenius.agent.client.Versions;
import com.contentgenius.agent.config.HotTopicSearcher;
import com.contentgenius.agent.config.PromptBuilder;
import com.contentgenius.agent.config.QdrantStorage;
import com.contentgenius.agent.config.RedisQueueService;
import com.contentgenius.agent.dto.AgentChatResponse;
import com.contentgenius.agent.dto.ContentVersionDto;
import com.contentgenius.agent.dto.TemplateDto;
import com.contentgenius.agent.dto.VersionRequest;
import com.contentgenius.agent.llm.LlmApiException;
import com.contentgenius.agent.llm.LlmErrorClassifier;
import com.contentgenius.agent.model.ChatMode;
import com.contentgenius.agent.model.RouteType;
import com.contentgenius.agent.writer.ArticleWriter;
import com.contentgenius.agent.writer.SmartRouter;
import com.contentgenius.agent.writer.SmartRouter.WriteIntent;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.stream.Collectors;

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
    private final HotTopicSearcher hotTopicSearcher;
    //向量
    private final QdrantStorage qdrantStorage;
    //路由
    private final SmartRouter smartRouter;
    //redis记忆
    private final RedisQueueService redisQueueService;
//chat模式改写
    public AgentChatResponse chat(Long projectId, String topic, String platform,
                                  Boolean isopen, Boolean useRag, ChatMode mode, Integer memoryId) {
        //先拼一套提示词
        ChatContext ctx = buildChatContext(projectId, topic, platform, isopen, useRag, mode, memoryId);
        //选择模式
        ArticleDraft draft = ctx.getMode() == ChatMode.THINK
                ? executeThinkSync(ctx)
                : executeFastSync(ctx);
        //存草稿
        ContentVersionDto saved = saveVersion(
                ctx.getProjectId(), ctx.getTopic(), draft.getContent(),
                ctx.getPlatform(), ctx.getAuthorization(), draft.getTitle());
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        redisQueueService.loadmemoryid(
                memoryId != null ? String.valueOf(memoryId) : null,
                String.valueOf(userId),
                saved.getContent());

        //返回结果
        return new AgentChatResponse(
                draft.getContent(), ctx.getPlatform(), saved.getId(), saved.getVersionNo(), ctx.getMode().getCode());
    }
    /**
     * TokenStream 真流式：
     * <p>onPartialResponse 推 token，onCompleteResponse 落库并发 done，onError 处理 fallback。
     */
    public Flux<AgentChatResponse> streamChat(Long projectId, String topic, String platform,
                                             Boolean isopen, Boolean useRag, ChatMode mode, Integer memoryId) {
        ChatContext ctx = buildChatContext(projectId, topic, platform, isopen, useRag, mode, memoryId);
        if (ctx.getMode() == ChatMode.THINK) {
            log.info("思考模式完整四步仅支持同步 /chat；流式按快速模式输出正文");
        }
        String systemPrompt = ctx.getSystemPrompt();
        String userPrompt = ctx.getUserPrompt();
        String resolvedPlatform = ctx.getPlatform();
        String authorization = ctx.getAuthorization();
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
                                projectId, topic, resolvedPlatform, authorization, ctx.getMode(),
                                fullContent, emittedPartial, sink);
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
                                               ChatMode mode,
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
                            sink.next(new AgentChatResponse(partial, platform, null, null, mode.getCode()));
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
                            ContentVersionDto saved = saveVersion(
                                    projectId, topic, fullContent.toString(), platform, authorization, null);
                            // C3) 发送 done 事件：content 置空，仅携带版本信息
                            sink.next(new AgentChatResponse("", platform, saved.getId(), saved.getVersionNo(), mode.getCode()));
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
                                        projectId, topic, platform, authorization, mode,
                                        fullContent, emittedPartial, sink);
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
    //拼提示词
    private ChatContext buildChatContext(Long projectId, String topic, String platform,
                                         Boolean isopen, Boolean useRag, ChatMode mode,Integer memoryid) {
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";
        String promptHint = resolvePromptHintByPriority(resolvedPlatform);
        String webContext = Boolean.TRUE.equals(isopen) ? hotTopicSearcher.search(topic) : null;
        String ragContext = resolveRagContext(resolvedPlatform, topic, useRag);
        String systemPrompt = promptBuilder.buildSystemPrompt(
                resolvedPlatform, promptHint, webContext, ragContext);
        String userPrompt = promptBuilder.buildUserPrompt(topic);
        String authorization = currentAuthorizationHeader();
        ChatMode resolvedMode = mode == null ? ChatMode.FAST : mode;

        return new ChatContext(projectId, topic, resolvedPlatform, systemPrompt, userPrompt, authorization, resolvedMode, memoryid);
    }

    /** 快速模式：一步 write */
    private ArticleDraft executeFastSync(ChatContext ctx) {
        String content = articleWriter.writeArticleWithFallback(ctx.getSystemPrompt(), ctx.getUserPrompt());
        return new ArticleDraft(content, null);
    }


    private ArticleDraft executeThinkSync(ChatContext ctx) {
        WriteIntent intent = smartRouter.resolveIntent(ctx.getTopic()); // 获取意图枚举
        log.info("思考模式 intent={} topic={}", intent, ctx.getTopic());
        if (intent == WriteIntent.OUTLINE) {
            // 如果是大纲则从头写
            return executeThinkFullPipeline(ctx);
        }
        //如果是重写
        if (intent == WriteIntent.REWRITE) {
            String previousDraft = requirePreviousDraft(ctx);
            String content = articleWriter.rewrite(
                    ctx.getSystemPrompt(), buildRewritePayload(ctx.getTopic(), previousDraft));
            return new ArticleDraft(content, null);
        }
        //如果是润色
        if (intent == WriteIntent.STYLE) {
            String previousDraft = requirePreviousDraft(ctx);
            String content = articleWriter.style(ctx.getSystemPrompt(), previousDraft);
            return new ArticleDraft(content, null);
        }
        if (intent == WriteIntent.TITLE) {
            String previousDraft = requirePreviousDraft(ctx);//历史稿件
            String titleStr = articleWriter.title(ctx.getSystemPrompt(), previousDraft);//拿模板跟稿件拼
            return new ArticleDraft(previousDraft, titleStr);
        }
        return executeThinkFullPipeline(ctx);
    }

    /** 思考模式 · 从零写：outline → write → style → title */
    private ArticleDraft executeThinkFullPipeline(ChatContext ctx) {
        String outlineText = articleWriter.outline(ctx.getSystemPrompt(), ctx.getUserPrompt());
        String writerUserPrompt = buildWriterUserPrompt(ctx.getTopic(), outlineText);
        String draft = articleWriter.writeArticleWithFallback(ctx.getSystemPrompt(), writerUserPrompt);
        String polished = articleWriter.style(ctx.getSystemPrompt(), draft);
        String titleStr = articleWriter.title(ctx.getSystemPrompt(), polished);
        return new ArticleDraft(polished, titleStr);
    }

    private static String buildWriterUserPrompt(String topic, String outlineText) {
        return "创作主题：" + topic + "\n\n请严格按以下大纲撰写完整正文：\n" + outlineText;
    }

    private static String buildRewritePayload(String topic, String previousDraft) {
        return "【原文】\n" + previousDraft + "\n\n【修改要求】\n" + topic;
    }

    /** 从 Redis 读取上一轮成稿；rewrite / style / title 依赖此内容 */
    private String requirePreviousDraft(ChatContext ctx) {
        if (ctx.getMemoryId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "多轮改稿需要 memoryId，请先完成首次创作");
        }
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();//拿userid
        String previousDraft = redisQueueService.getMemoryContent(
                String.valueOf(ctx.getMemoryId()), String.valueOf(userId));//拿历史稿件
        if (!StringUtils.hasText(previousDraft)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到上一轮稿件，请先完成首次创作");
        }
        return previousDraft;
    }

    @Value
    private static class ArticleDraft {
        String content;
        String title;
    }

//存稿内部方法
    private ContentVersionDto saveVersion(Long projectId, String topic, String content, String platform,
                                          String authorization, String titleOverride) {
        VersionRequest request = new VersionRequest();
        request.setTitle(StringUtils.hasText(titleOverride) ? titleOverride.trim() : buildTitle(topic));
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


    private String resolveRagContext(String platform, String topic, Boolean useRag) {
        if (!Boolean.TRUE.equals(useRag)) {
            return null;
        }
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        List<TextSegment> segments = qdrantStorage.search(platform, userId, topic);
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        String joined = segments.stream()
                .map(TextSegment::text)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n---\n"));
        return StringUtils.hasText(joined) ? joined : null;
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
