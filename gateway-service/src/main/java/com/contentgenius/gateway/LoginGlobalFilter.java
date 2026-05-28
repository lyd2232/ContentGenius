package com.contentgenius.gateway;

import com.contentgenius.common.util.JwtUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
/**
 * 网关层面设置白名单，解析token验证token
 */
public class LoginGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtils jwtUtils;

    //白名单
    private List<String> skipAuthUrls;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求头并判断白名单
        String url = exchange.getRequest().getURI().getPath();
        if (skipAuthUrls != null && isSkipUrl(url)) {
            //放行
            return chain.filter(exchange);
        }
//获取请求头
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        //判断请求头是否空
        if (!StringUtils.hasText(auth)) {

            return unauthorized(exchange);
        }
//获取token
        String token = auth.startsWith("Bearer ") ? auth.substring(7).trim() : auth.trim();
       //校验合法性
        if (!jwtUtils.verify(token)) {
            //返回格式
            return unauthorized(exchange);
        }
//放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
//判断是否是跳过认证的url
    public boolean isSkipUrl(String url) {
        for (String skipAuthUrl : skipAuthUrls) {
            if (url.startsWith(skipAuthUrl)) {
                return true;
            }
        }
        return false;
    }
//401问题返回
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        byte[] body = "{\"code\":401,\"message\":\"未登录或 token 无效\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
