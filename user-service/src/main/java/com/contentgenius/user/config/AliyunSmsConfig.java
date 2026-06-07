package com.contentgenius.user.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云号码认证
 */
@Configuration
@ConditionalOnProperty(prefix = "contentgenius.sms", name = "access-key-id")
public class AliyunSmsConfig {

    private static final String ENDPOINT = "dypnsapi.aliyuncs.com";

    @Bean
    public Client aliyunDypnsClient(SmsProperties smsProperties) throws Exception {
        Config config = new Config()
                .setAccessKeyId(smsProperties.getAccessKeyId())
                .setAccessKeySecret(smsProperties.getAccessKeySecret());
        config.endpoint = ENDPOINT;
        return new Client(config);
    }
}

