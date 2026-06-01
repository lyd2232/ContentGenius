package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.models.web-search")
public class WebSearchProperties {
    private String endpoint;
    private String apiKey;
    private String timeout;
}
