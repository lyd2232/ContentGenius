package com.contentgenius.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
//配置springsercity规则
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // 关闭CSRF防护，适用于无状态的JWT认证场景
                .csrf(csrf -> csrf.disable())
// 禁用默认的表单登录功能，使用自定义的JWT认证
                .formLogin(form -> form.disable())
// 禁用HTTP Basic认证方式
                .httpBasic(basic -> basic.disable())
// 配置Session创建策略为无状态模式，不创建和使用HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
// 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 注册和登录接口允许匿名访问
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/api/users/sms/send"
                        ).permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
