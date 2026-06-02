package com.contentgenius.agent;

import com.contentgenius.agent.config.JwtAuthenticationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.contentgenius.agent", "com.contentgenius.common"})
@EnableFeignClients(basePackages = "com.contentgenius.agent.client")
@EnableScheduling
@EnableConfigurationProperties(JwtAuthenticationFilter.class)
public class AgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
