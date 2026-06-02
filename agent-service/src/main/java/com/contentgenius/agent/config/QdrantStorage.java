package com.contentgenius.agent.config;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Component
@RequiredArgsConstructor
public class QdrantStorage {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    //搜索
    public List<TextSegment> search(String platform, Long userId, String topic) {
        Embedding queryEmbedding = embeddingModel.embed(topic).content();//获取向量相似度
//按照规定里面找
        Filter filter = metadataKey("userId")
                .isEqualTo(String.valueOf(userId))//筛选出指定用户
                .and(metadataKey("platform").isEqualTo(platform));//筛选出指定平台
//获取返回值以及最小相似度
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.7)
                .filter(filter)//只查指定过滤方式
                .build();
//构建返回文本
        return embeddingStore.search(request).matches().stream()
                .map(EmbeddingMatch::embedded)
                .toList();
    }

    //写入
    public void ingest(String platform, Long userId, Long versionId, String content) {
        Metadata metadata = Metadata.from(Map.of(//构建用于向量库内隔离
                "userId", String.valueOf(userId),
                "platform", platform,
                "versionId", versionId
        ));
        TextSegment segment = TextSegment.from(content, metadata);//将文章与元素转为textsegment
        Embedding embedding = embeddingModel.embed(segment).content();//把textsegment转为向量
        embeddingStore.add(embedding, segment);//存向量
    }
}
