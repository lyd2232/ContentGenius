package com.contentgenius.agent.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 出站拦截器：把用户调用 agent 时带的 JWT 原样传给 content-service。
 * <p>否则 content 的 JwtAuthenticationFilter 会 401，拉不到 template。
 */
@Component
//实现RequestInterceptor，重写apply方法实现每个接口出战都自动调一次apply方法添加token省的到其他的模块不认人
public class FeignAuthRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 取出当前 Tomcat 线程正在处理的 HTTP 请求上下文
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 非 Web 调用（如定时任务）时没有上下文，直接跳过
        if (attributes == null) {
            return;
        }
        // 读取前端/Gateway 传来的 Authorization
        String authorization = attributes.getRequest().getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            // 写入 Feign 即将发往 content-service 的请求头
            template.header("Authorization", authorization);
        }
    }
}
