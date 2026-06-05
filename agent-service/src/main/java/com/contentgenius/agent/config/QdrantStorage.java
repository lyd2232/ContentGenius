package com.contentgenius.agent.config;

import com.contentgenius.agent.dto.RagSimilarityHit;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static io.qdrant.client.ConditionFactory.matchKeyword;

/**
 * Qdrant 向量读写；payload 中 userId、versionId、platform 均为字符串。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantStorage {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;

    private static final double RAG_MIN_SCORE = 0.65;

    // 搜索
    public List<TextSegment> search(String platform, Long userId, String topic) {
        Embedding queryEmbedding = embeddingModel.embed(topic).content(); // 获取向量
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(RAG_MIN_SCORE)
                .filter(langChainFilter(platform, userId)) // 按 userId + platform 过滤
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        List<TextSegment> segments = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            segments.add(match.embedded());
        }
        if (segments.isEmpty()) {
            logRagMissDiagnostic(platform, userId, topic, queryEmbedding);
        }
        return segments;
    }

    /** 未命中时打出最高分，便于区分「没入库」与「分数不够」 */
    private void logRagMissDiagnostic(String platform, Long userId, String topic, Embedding queryEmbedding) {
        EmbeddingSearchRequest diag = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .minScore(0.0)
                .filter(langChainFilter(platform, userId))
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(diag).matches();
        if (matches.isEmpty()) {
            log.info("RAG 诊断：Qdrant 中无 userId={} platform={} 的已定稿向量（可能未点定稿或入库失败）",
                    userId, normalizePlatform(platform));
            return;
        }
        EmbeddingMatch<TextSegment> top = matches.get(0);
        Object versionId = top.embedded().metadata().toMap().get("versionId");
        log.info("RAG 诊断：ragQuery={} 最高相似分={}（阈值 {}）versionId={}",
                topic, top.score(), RAG_MIN_SCORE, versionId);
    }

    // 相似度检索 0.92
    public List<RagSimilarityHit> simsearch(String platform, Long userId, String content, Long excludeVersionId) {
        Embedding queryEmbedding = embeddingModel.embed(content).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.92)
                .filter(langChainFilter(platform, userId))
                .build();
        String excludeVersionIdStr = excludeVersionId == null ? null : String.valueOf(excludeVersionId);
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        List<RagSimilarityHit> hits = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            RagSimilarityHit hit = toSimilarityHit(match, excludeVersionIdStr);// 转为相似度检索结果
            if (hit != null) {
                hits.add(hit);
            }
        }
        return hits;
    }

    // 写入
    public void ingest(String platform, Long userId, Long versionId, String content) {
        Metadata metadata = Metadata.from(buildMetadata(platform, userId, versionId)); // 向量库隔离字段
        TextSegment segment = TextSegment.from(content, metadata);
        Embedding embedding = embeddingModel.embed(segment).content(); // 正文转向量
        embeddingStore.add(embedding, segment); // 存向量
    }

    // 删除
    public void deleteByVersionId(Long userId, Long versionId) {
        String userIdStr = String.valueOf(userId);
        String versionIdStr = String.valueOf(versionId);
        String collection = qdrantProperties.getCollection();
        Common.Filter filter = Common.Filter.newBuilder()
                .addMust(matchKeyword("userId", userIdStr))
                .addMust(matchKeyword("versionId", versionIdStr))
                .build();
        try {
            deleteByFilter(collection, filter);
            log.info("已删除 Qdrant 向量 userId={} versionId={}", userIdStr, versionIdStr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("删除向量被中断", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("删除 Qdrant 向量失败 versionId=" + versionIdStr, e.getCause());
        }
    }

    private void deleteByFilter(String collection, Common.Filter filter)
            throws InterruptedException, ExecutionException {
        qdrantClient.deleteAsync(collection, filter).get();
    }

    // LangChain4j 检索过滤
    private static Filter langChainFilter(String platform, Long userId) {
        return metadataKey("userId")
                .isEqualTo(String.valueOf(userId)) // 筛选指定用户
                .and(metadataKey("platform").isEqualTo(normalizePlatform(platform))); // 筛选指定平台
    }

    // 写入 Qdrant 的 metadata
    private static Map<String, Object> buildMetadata(String platform, Long userId, Long versionId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", String.valueOf(userId));
        metadata.put("platform", normalizePlatform(platform));
        metadata.put("versionId", String.valueOf(versionId));
        return metadata;
    }

    // 构建平台默认小红书
    private static String normalizePlatform(String platform) {
        return StringUtils.hasText(platform) ? platform.trim() : "xiaohongshu";
    }
    //获取命中的列表
    private static RagSimilarityHit toSimilarityHit(EmbeddingMatch<TextSegment> match, String excludeVersionIdStr) {
        Object raw = match.embedded().metadata().toMap().get("versionId");
        Long versionId = parseVersionId(raw == null ? null : String.valueOf(raw));
        if (versionId == null) {
            return null;
        }
        if (excludeVersionIdStr != null && excludeVersionIdStr.equals(String.valueOf(versionId))) {
            return null; // 排除当前版本
        }
        //拼接返回格式
        return new RagSimilarityHit(versionId, match.score(), match.embedded().text());
    }

    // metadata 中 versionId 为字符串，转成 Long 供 API 使用
    private static Long parseVersionId(String versionIdStr) {
        if (!StringUtils.hasText(versionIdStr)) {
            return null;
        }
        try {
            return Long.parseLong(versionIdStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
