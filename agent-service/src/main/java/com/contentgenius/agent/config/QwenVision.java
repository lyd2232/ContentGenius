package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.models.qwen3-vl-plus")
public class QwenVision {
    private String provider;
    private String apiKey;
    private String modelName;
    private String endpoint;
    private Double temperature;
    private Integer maxTokens;
}
