package com.contentgenius.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "contentgenius.prompt")
public class PromptProperties {

    private String baseInstruction;

    private Map<String, String> platform = new HashMap<>();
}