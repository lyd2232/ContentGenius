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
    public AgentChatResponse chat(Long projectId, String creationTheme, String topic, String platform,
                                  Boolean isopen, Boolean useRag, ChatMode mode, Integer memoryId,
                                  String thinkAction) {
        ChatContext ctx = buildChatContext(projectId, creationTheme, topic, platform, isopen, useRag, mode, memoryId, thinkAction);
        return finishChat(ctx, executeDraft(ctx));
    }

    private AgentChatResponse finishChat(ChatContext ctx, ArticleDraft draft) {
        ContentVersionDto saved = saveVersion(
                ctx.getProjectId(), resolveCreationTheme(ctx), draft.getContent(),
                ctx.getPlatform(), ctx.getAuthorization(), draft.getTitle());
        int effectiveMemoryId = resolveEffectiveMemoryId(ctx.getMemoryId(), saved.getId());
        persistDraftMemory(effectiveMemoryId, ctx.getUserId(), saved.getContent(), ctx.getCreationTheme());
        AgentChatResponse response = new AgentChatResponse(
                draft.getContent(), ctx.getPlatform(), saved.getId(), saved.getVersionNo(), ctx.getMode().getCode());
        response.setMemoryId(effectiveMemoryId);
        return response;
    }

    private ArticleDraft executeDraft(ChatContext ctx) {
        return ctx.getMode().usesThinkPipeline() ? executeThinkSync(ctx) : executeFastSync(ctx);
    }
    /**
     * TokenStream 真流式：
     * <p>onPartialResponse 推 token，onCompleteResponse 落库并发 done，onError 处理 fallback。
     */
    public Flux<AgentChatResponse> streamChat(Long projectId, String creationTheme, String topic, String platform,
                                             Boolean isopen, Boolean useRag, ChatMode mode, Integer memoryId,
                                             String thinkAction) {
        ChatContext ctx = buildChatContext(projectId, creationTheme, topic, platform, isopen, useRag, mode, memoryId, thinkAction);
        if (ctx.getMode().usesThinkPipeline()) {
            return streamThinkSync(ctx);
        }
        if (shouldReviseFromPreviousDraft(ctx)) {
            return streamFastReviseSync(ctx);
        }
        String systemPrompt = ctx.getSystemPrompt();
        String userPrompt = ctx.getUserPrompt();
        String resolvedPlatform = ctx.getPlatform();
        String authorization = ctx.getAuthorization();
        AtomicBoolean emittedPartial = new AtomicBoolean(false);
        StringBuilder fullContent = new StringBuilder();
        Long userId = currentUserId();

        return Flux.<AgentChatResponse>create(sink ->
                        streamWithTokenStreamFallback(RouteType.ARTICLE, systemPrompt, userPrompt,
                                projectId, ctx.getCreationTheme(), topic, resolvedPlatform, authorization,
                                ctx.getMode(), memoryId, userId, fullContent, emittedPartial, sink))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 快速模式改稿：有 Redis 稿件时走改写，一次性推送（避免流式回调丢登录态） */
    private Flux<AgentChatResponse> streamFastReviseSync(ChatContext ctx) {
        return streamThinkSync(ctx);
    }

    /** 思考模式：流式接口也走同步四步编排，一次性推送成稿 */
    private Flux<AgentChatResponse> streamThinkSync(ChatContext ctx) {
        return Flux.<AgentChatResponse>create(sink -> Schedulers.boundedElastic().schedule(() -> {
            try {
                if (sink.isCancelled()) {
                    return;
                }
                AgentChatResponse done = finishChat(ctx, executeDraft(ctx));
                sink.next(new AgentChatResponse(done.getContent(), done.getPlatform(), null, null, done.getMode()));
                AgentChatResponse tail = new AgentChatResponse(
                        "", done.getPlatform(), done.getVersionId(), done.getVersionNo(), done.getMode());
                tail.setMemoryId(done.getMemoryId());
                sink.next(tail);
                sink.complete();
            } catch (Exception ex) {
                sink.error(ex);
            }
        })).subscribeOn(Schedulers.boundedElastic());
    }

    private void streamWithTokenStreamFallback(RouteType routeType,
                                               String systemPrompt,
                                               String userPrompt,
                                               Long projectId,
                                               String creationTheme,
                                               String topic,
                                               String platform,
                                               String authorization,
                                               ChatMode mode,
                                               Integer memoryId,
                                               Long userId,
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
                                    projectId, creationTheme, fullContent.toString(), platform, authorization, null);
                            int effectiveMemoryId = resolveEffectiveMemoryId(memoryId, saved.getId());
                            persistDraftMemory(effectiveMemoryId, userId, fullContent.toString(), creationTheme);
                            AgentChatResponse done = new AgentChatResponse(
                                    "", platform, saved.getId(), saved.getVersionNo(), mode.getCode());
                            done.setMemoryId(effectiveMemoryId);
                            sink.next(done);
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
                                        projectId, creationTheme, topic, platform, authorization, mode, memoryId,
                                        userId, fullContent, emittedPartial, sink);
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
    private ChatContext buildChatContext(Long projectId, String creationTheme, String topic, String platform,
                                         Boolean isopen, Boolean useRag, ChatMode mode, Integer memoryid,
                                         String thinkAction) {
        ChatMode resolvedMode = mode == null ? ChatMode.FAST : mode;
        if (resolvedMode.usesThinkPipeline()) {
            isopen = false;
            useRag = false;
        }
        String resolvedPlatform = StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";
        String theme = StringUtils.hasText(creationTheme) ? creationTheme.trim()
                : (StringUtils.hasText(topic) ? topic.trim() : "");
        String instruction = StringUtils.hasText(topic) ? topic.trim() : theme;
        Long userId = currentUserId();
        String promptHint = resolvePromptHintByPriority(resolvedPlatform);
        String webContext = Boolean.TRUE.equals(isopen) ? hotTopicSearcher.search(theme) : null;
        String ragContext = resolveRagContext(resolvedPlatform, theme, instruction, useRag, userId);
        String systemPrompt = promptBuilder.buildSystemPrompt(
                resolvedPlatform, promptHint, webContext, ragContext);
        String userPrompt = promptBuilder.buildUserPromptWithRequirement(theme, instruction);
        String authorization = currentAuthorizationHeader();

        return new ChatContext(projectId, theme, instruction, resolvedPlatform, systemPrompt, userPrompt,
                authorization, resolvedMode, memoryid, normalizeThinkAction(thinkAction), userId);
    }

    private static String normalizeThinkAction(String thinkAction) {
        if (!StringUtils.hasText(thinkAction)) {
            return null;
        }
        return thinkAction.trim().toLowerCase();
    }

    private static Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private static int resolveEffectiveMemoryId(Integer requestMemoryId, Long versionId) {
        if (requestMemoryId != null) {
            return requestMemoryId;
        }
        if (versionId == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "存稿成功但缺少 versionId，无法建立会话记忆");
        }
        return versionId.intValue();
    }

    private void persistDraftMemory(int memoryId, Long userId, String content, String creationTheme) {
        redisQueueService.loadmemoryid(String.valueOf(memoryId), String.valueOf(userId), content);
        if (StringUtils.hasText(creationTheme)) {
            String theme = creationTheme.trim();
            redisQueueService.saveMemoryTopic(String.valueOf(memoryId), String.valueOf(userId), theme);
            log.info("会话主题已写入 Redis memoryId={} sessionTopic={}", memoryId, theme);
        }
        log.info("稿件记忆已写入 Redis memoryId={} userId={}", memoryId, userId);
    }

    /** 快速模式：有上轮稿件且主题未变则改写，否则按「主题+要求」从零写 */
    private ArticleDraft executeFastSync(ChatContext ctx) {
        String previousDraft = loadPreviousDraft(ctx);
        if (shouldReviseFromPreviousDraft(ctx)) {
            log.info("快速模式改稿 memoryId={} creationTheme={} instruction={}",
                    ctx.getMemoryId(), resolveCreationTheme(ctx), ctx.getTopic());
            String content = articleWriter.rewrite(
                    ctx.getSystemPrompt(), buildRewritePayload(resolveReviseInstruction(ctx), previousDraft));
            return new ArticleDraft(content, null);
        }
        log.info("快速模式首版 creationTheme={} instruction={}", resolveCreationTheme(ctx), ctx.getTopic());
        String content = articleWriter.writeArticleWithFallback(ctx.getSystemPrompt(), ctx.getUserPrompt());
        return new ArticleDraft(content, null);
    }

    /** 仅当 memoryId 有旧稿且创作主题与 Redis 记录一致时才走改稿 */
    private boolean shouldReviseFromPreviousDraft(ChatContext ctx) {
        if (ctx.getMemoryId() == null) {
            return false;
        }
        String previousDraft = loadPreviousDraft(ctx);
        if (!StringUtils.hasText(previousDraft)) {
            return false;
        }
        if (StringUtils.hasText(ctx.getCreationTheme())) {
            String currentTheme = ctx.getCreationTheme().trim();
            String storedTheme = redisQueueService.getMemoryTopic(
                    String.valueOf(ctx.getMemoryId()), String.valueOf(ctx.getUserId()));
            if (StringUtils.hasText(storedTheme) && !storedTheme.equals(currentTheme)) {
                log.info("创作主题已变更 {} -> {}，忽略旧稿按首版生成", storedTheme, currentTheme);
                return false;
            }
        }
        return true;
    }

    private String loadPreviousDraft(ChatContext ctx) {
        if (ctx.getMemoryId() == null) {
            return null;
        }
        return redisQueueService.getMemoryContent(
                String.valueOf(ctx.getMemoryId()), String.valueOf(ctx.getUserId()));
    }

    private ArticleDraft executeThinkSync(ChatContext ctx) {
        WriteIntent intent = resolveThinkIntent(ctx);
        log.info("mode={} intent={} creationTheme={} instruction={} memoryId={}",
                ctx.getMode().getCode(), intent, ctx.getCreationTheme(), ctx.getTopic(), ctx.getMemoryId());
        if (intent == WriteIntent.OUTLINE) {
            // 如果是大纲则从头写
            return executeThinkFullPipeline(ctx);
        }
        //如果是重写
        if (intent == WriteIntent.REWRITE) {
            String previousDraft = requirePreviousDraft(ctx);
            String content = articleWriter.rewrite(
                    ctx.getSystemPrompt(), buildRewritePayload(resolveReviseInstruction(ctx), previousDraft));
            return new ArticleDraft(content, null);
        }
        //如果是润色
        if (intent == WriteIntent.STYLE) {
            String previousDraft = requirePreviousDraft(ctx);
            String content = articleWriter.style(ctx.getSystemPrompt(), buildStylePayload(ctx.getTopic(), previousDraft));
            return new ArticleDraft(content, null);
        }
        if (intent == WriteIntent.TITLE) {
            String previousDraft = requirePreviousDraft(ctx);
            String titleStr = articleWriter.title(ctx.getSystemPrompt(), buildTitlePayload(ctx.getTopic(), previousDraft));
            String body = mergeTitleIntoContent(titleStr, previousDraft);
            return new ArticleDraft(body, titleStr);
        }
        return executeThinkFullPipeline(ctx);
    }

    /** 思考模式 · 从零写：outline → write → style → title */
    private ArticleDraft executeThinkFullPipeline(ChatContext ctx) {
        String pipelineTopic = resolvePipelineTopic(ctx);
        String userPrompt = promptBuilder.buildUserPrompt(pipelineTopic);
        String outlineText = articleWriter.outline(ctx.getSystemPrompt(), userPrompt);
        String writerUserPrompt = buildWriterUserPrompt(pipelineTopic, outlineText);
        String draft = articleWriter.writeArticleWithFallback(ctx.getSystemPrompt(), writerUserPrompt);
        String polished = articleWriter.style(ctx.getSystemPrompt(), draft);
        String titleStr = articleWriter.title(ctx.getSystemPrompt(), polished);
        return new ArticleDraft(polished, titleStr);
    }

    /** 四步编排始终用 creationTheme；有独立本轮指令时拼入用户指令 */
    private String resolvePipelineTopic(ChatContext ctx) {
        String theme = resolveCreationTheme(ctx);
        if (!hasSeparateInstruction(ctx)) {
            return theme;
        }
        log.info("四步编排 creationTheme={} instruction={}", theme, ctx.getTopic().trim());
        return theme + "。用户指令：" + ctx.getTopic().trim();
    }

    private String resolveCreationTheme(ChatContext ctx) {
        if (StringUtils.hasText(ctx.getCreationTheme())) {
            return ctx.getCreationTheme().trim();
        }
        if (ctx.getMemoryId() != null) {
            String stored = redisQueueService.getMemoryTopic(
                    String.valueOf(ctx.getMemoryId()), String.valueOf(ctx.getUserId()));
            if (StringUtils.hasText(stored)) {
                return stored.trim();
            }
        }
        return StringUtils.hasText(ctx.getTopic()) ? ctx.getTopic().trim() : "";
    }

    private static boolean hasSeparateInstruction(ChatContext ctx) {
        if (!StringUtils.hasText(ctx.getTopic())) {
            return false;
        }
        String instruction = ctx.getTopic().trim();
        String theme = StringUtils.hasText(ctx.getCreationTheme()) ? ctx.getCreationTheme().trim() : instruction;
        return !instruction.equals(theme);
    }

    private static String buildWriterUserPrompt(String topic, String outlineText) {
        return "创作主题：" + topic + "\n\n请严格按以下大纲撰写完整正文：\n" + outlineText;
    }

    private String resolveReviseInstruction(ChatContext ctx) {
        return promptBuilder.buildReviseRequirementBlock(resolveCreationTheme(ctx), ctx.getTopic());
    }

    private static String buildRewritePayload(String reviseInstruction, String previousDraft) {
        return "【原文】\n" + previousDraft + "\n\n【修改要求】\n" + reviseInstruction;
    }

    private static String buildStylePayload(String topic, String previousDraft) {
        if (!StringUtils.hasText(topic)) {
            return previousDraft;
        }
        return "【润色要求】\n" + topic.trim() + "\n\n【待润色正文】\n" + previousDraft;
    }

    private static String buildTitlePayload(String topic, String previousDraft) {
        if (!StringUtils.hasText(topic)) {
            return previousDraft;
        }
        return "【标题要求】\n" + topic.trim() + "\n\n【正文】\n" + previousDraft;
    }

    /**
     * 意图：思考模式显式 thinkAction 优先；智能路由对本轮 instruction 走 SmartRouter。
     */
    private WriteIntent resolveThinkIntent(ChatContext ctx) {
        if (ctx.getMode() != ChatMode.SMART) {
            WriteIntent explicit = parseThinkActionIntent(ctx.getThinkAction());
            if (explicit != null) {
                log.info("思考模式显式动作 thinkAction={} intent={}", ctx.getThinkAction(), explicit);
                return explicit;
            }
        }
        WriteIntent intent = smartRouter.resolveIntent(ctx.getTopic());
        if (ctx.getMode() == ChatMode.SMART) {
            log.info("智能路由 SmartRouter intent={} instruction={}", intent, ctx.getTopic());
        }
        return intent;
    }

    private static WriteIntent parseThinkActionIntent(String thinkAction) {
        if (!StringUtils.hasText(thinkAction)) {
            return null;
        }
        return switch (thinkAction.trim().toLowerCase()) {
            case "title" -> WriteIntent.TITLE;
            case "outline" -> WriteIntent.OUTLINE;
            case "rewrite" -> WriteIntent.REWRITE;
            case "style" -> WriteIntent.STYLE;
            default -> null;
        };
    }

    /** 将新标题写入正文首行，便于预览与 Redis 记忆一致 */
    private static String mergeTitleIntoContent(String titleStr, String previousDraft) {
        if (!StringUtils.hasText(previousDraft)) {
            return StringUtils.hasText(titleStr) ? "# " + titleStr.trim() : "";
        }
        if (!StringUtils.hasText(titleStr)) {
            return previousDraft;
        }
        return "# " + titleStr.trim() + "\n\n" + stripLeadingMarkdownTitle(previousDraft);
    }

    private static String stripLeadingMarkdownTitle(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("#")) {
            return trimmed;
        }
        int lineEnd = trimmed.indexOf('\n');
        if (lineEnd < 0) {
            return "";
        }
        return trimmed.substring(lineEnd + 1).trim();
    }

    /** 从 Redis 读取上一轮成稿；rewrite / style / title 依赖此内容 */
    private String requirePreviousDraft(ChatContext ctx) {
        if (ctx.getMemoryId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "多轮改稿需要 memoryId，请先完成首次创作");
        }
        String previousDraft = redisQueueService.getMemoryContent(
                String.valueOf(ctx.getMemoryId()), String.valueOf(ctx.getUserId()));
        if (!StringUtils.hasText(previousDraft)) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "未找到上一轮稿件（memoryId=" + ctx.getMemoryId() + "），请先完成首次创作");
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


    private String resolveRagContext(String platform, String creationTheme, String instruction, Boolean useRag, Long userId) {
        if (!Boolean.TRUE.equals(useRag)) {
            return null;
        }
        String ragQuery = buildRagQuery(creationTheme, instruction);
        if (!StringUtils.hasText(ragQuery)) {
            return null;
        }
        try {
            List<TextSegment> segments = qdrantStorage.search(platform, userId, ragQuery);
            if (segments == null || segments.isEmpty()) {
                log.info("RAG 未命中已定稿参考 ragQuery={} platform={} userId={}（无向量或相似度<0.65；请确认已定稿且平台一致）",
                        ragQuery, platform, userId);
                return null;
            }
            String joined = segments.stream()
                    .map(TextSegment::text)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n---\n"));
            if (!StringUtils.hasText(joined)) {
                return null;
            }
            log.info("RAG 命中 {} 条参考片段 ragQuery={} platform={} userId={}", segments.size(), ragQuery, platform, userId);
            return joined;
        } catch (Exception ex) {
            log.warn("RAG 检索失败，跳过历史参考 ragQuery={} platform={} userId={}: {}",
                    ragQuery, platform, userId, ex.getMessage());
            return null;
        }
    }

    /** 检索词 = 创作主题 + 本轮要求，避免只用四字主题对不上长正文向量 */
    private static String buildRagQuery(String creationTheme, String instruction) {
        if (!StringUtils.hasText(creationTheme)) {
            return StringUtils.hasText(instruction) ? instruction.trim() : null;
        }
        String theme = creationTheme.trim();
        if (!StringUtils.hasText(instruction) || instruction.trim().equals(theme)) {
            return theme;
        }
        return theme + " " + instruction.trim();
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
