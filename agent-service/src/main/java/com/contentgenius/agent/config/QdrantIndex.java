package com.contentgenius.agent.config;


import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//创建集合
@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantIndex {

    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;

    private static final int DEFAULT_VECTOR_SIZE = 1024;

    @PostConstruct
    public void ensureCollection() {
        String collectionName = qdrantProperties.getCollection();//集合名称
        if (!StringUtils.hasText(collectionName)) {
            throw new IllegalStateException("contentgenius.rag.qdrant.collection 未配置");
        }
        int vectorSize = qdrantProperties.getVectorSize() != null
                ? qdrantProperties.getVectorSize()
                : DEFAULT_VECTOR_SIZE;

        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()//创建向量集合
                .setDistance(Collections.Distance.Cosine)
                .setSize(vectorSize)
                .build();
        try {
            Boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (Boolean.TRUE.equals(exists)) {
                log.info("Qdrant collection 已存在，跳过创建: {}（若改 embedding 维度须删库重建）", collectionName);
                return;
            }
            qdrantClient.createCollectionAsync(collectionName, vectorParams).get();
            log.info("Qdrant collection 创建成功: name={}, vectorSize={}, distance=Cosine",
                    collectionName, vectorSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("初始化 Qdrant collection 被中断: " + collectionName, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(
                    "初始化 Qdrant collection 失败: " + collectionName, e.getCause());
        }
    }
}
