package com.contentgenius.content;

import com.contentgenius.content.config.JwtAuthenticationFilter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.contentgenius.content", "com.contentgenius.common"})
@MapperScan("com.contentgenius.content.mapper")
@EnableConfigurationProperties(JwtAuthenticationFilter.class)
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}
