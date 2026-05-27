package com.contentgenius.user.config;

import com.contentgenius.common.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    //白名单
    private List<String> skipAuthUrls;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        //获取请求头并判断白名单
        String url = request.getRequestURI();
        if (skipAuthUrls != null && isSkipUrl(url)) {
            //放行
            chain.doFilter(request, response);
            return;
        }
        //获取请求头
        String auth = request.getHeader("Authorization");
        //判断请求头是否空
        if (!StringUtils.hasText(auth)) {
            unauthorized(response);
            return;
        }
        //获取token
        String token = auth.startsWith("Bearer ") ? auth.substring(7).trim() : auth.trim();
        //校验合法性
        if (!jwtUtils.verify(token)) {
            //返回格式
            unauthorized(response);
            return;
        }
        // 写入 SecurityContext，供 .authenticated() 使用（Gateway 过滤器无此步）
        String username = jwtUtils.getUsername(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //放行
        chain.doFilter(request, response);
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

    //返回格式
    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        byte[] body = "{\"code\":401,\"message\":\"未登录或 token 无效\"}".getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(body);
    }
}
