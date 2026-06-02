package com.contentgenius.agent.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;


@Configuration
public class QdrantConfig {

    private static final Duration EMBEDDING_CONNECT_TIMEOUT = Duration.ofSeconds(15);


//创建QdrantClient连接qdrant
    @Bean
    public QdrantClient qdrantClient(QdrantProperties qdrantProperties) {
        boolean useTls = Boolean.TRUE.equals(qdrantProperties.getUseTls());
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                qdrantProperties.getHost(), qdrantProperties.getPort(), useTls);
        return new QdrantClient(grpcClientBuilder.build());
    }
    //操作向量
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantProperties qdrantProperties) {
        return QdrantEmbeddingStore.builder()
                .host(qdrantProperties.getHost())
                .port(qdrantProperties.getPort())
                .collectionName(qdrantProperties.getCollection())
                .build();
    }

   //用于把文字转为向量
    @Bean
    public EmbeddingModel embeddingModel(EmbeddingProperties embeddingProperties) {
        Duration timeout = embeddingProperties.getTimeout() != null
                ? embeddingProperties.getTimeout()
                : Duration.ofSeconds(60);

        OkHttpClientBuilder httpClientBuilder = new OkHttpClientBuilder()
                .okHttpClientBuilder(new OkHttpClient.Builder()
                        .connectTimeout(EMBEDDING_CONNECT_TIMEOUT)
                        .readTimeout(timeout));

        return OpenAiEmbeddingModel.builder()
                .httpClientBuilder(httpClientBuilder)
                .apiKey(embeddingProperties.getApiKey())
                .baseUrl(embeddingProperties.getBaseUrl())
                .modelName(embeddingProperties.getModel())
                .timeout(timeout)
                .maxRetries(0)
                .build();
    }
}
