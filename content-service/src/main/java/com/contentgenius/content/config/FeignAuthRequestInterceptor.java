package com.contentgenius.content.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 出站：把当前用户的 JWT 传给 agent-service。
 */
@Component
public class FeignAuthRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        String authorization = attributes.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            template.header("Authorization", authorization);
        }
    }
}
