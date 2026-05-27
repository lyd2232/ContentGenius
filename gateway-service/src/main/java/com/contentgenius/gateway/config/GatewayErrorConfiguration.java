package com.contentgenius.gateway.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * 注册自定义 ErrorWebExceptionHandler，优先级高于默认实现。
 */
@Configuration
public class GatewayErrorConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)//设置最高优先级
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes,
                                                             WebProperties webProperties,
                                                             ApplicationContext applicationContext,
                                                             ServerCodecConfigurer serverCodecConfigurer) {
        return new GatewayErrorWebExceptionHandler(
                errorAttributes, webProperties, applicationContext, serverCodecConfigurer);
    }
}
