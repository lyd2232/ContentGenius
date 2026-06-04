package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;


/**
 * 绑定模型
 */
@Data
@Primary
@ConfigurationProperties(prefix = "ai.models.qwen-max")
public class QwenMaxProperties {

    private String provider;
    private String apiKey;
    private String modelName;
    private String endpoint;
    private Double temperature;
    private Integer maxTokens;
}
