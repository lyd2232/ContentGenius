package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * 绑定模型
 */
@Data
@ConfigurationProperties(prefix = "ai.models.qwen-max")
public class QwenMaxProperties {

    private String provider;
    private String apiKey;
    private String modelName;
    private String endpoint;
    private Double temperature;
    private Integer maxTokens;
}
