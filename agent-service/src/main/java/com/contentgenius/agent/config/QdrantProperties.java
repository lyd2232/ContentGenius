package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "contentgenius.rag.qdrant")
public class QdrantProperties {
    private String host;
    private Integer port;
    /** Nacos: use-tls */
    private Boolean useTls;
    private String collection;
    /** 须与 Embedding 模型输出维度一致；text-embedding-v4 为 1024 */
    private Integer vectorSize;
}
