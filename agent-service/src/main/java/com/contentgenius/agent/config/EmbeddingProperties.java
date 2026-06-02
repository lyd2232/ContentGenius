package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "ai.embedding")
public class EmbeddingProperties {
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
    private Duration timeout;
}
