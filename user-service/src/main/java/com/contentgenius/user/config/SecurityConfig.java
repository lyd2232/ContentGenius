package com.contentgenius.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 开启 @PreAuthorize 注解（按需）
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF
                .csrf(csrf -> csrf.disable())
                // 2. 关闭默认表单登录页（不配置 FormLogin，或者明确禁用）
                .formLogin(form -> form.disable())
                // 如果还启用了 httpBasic，也可以一并禁用（根据需要）
                .httpBasic(basic -> basic.disable())
                // 3. Session 管理：无状态（JWT 不用 Session）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. 请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行注册和登录接口
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        // 其他所有请求都需要认证（如果调试阶段想暂时放行，可改为 .anyRequest().permitAll()）
                        .anyRequest().authenticated()
                )
                // 5. 添加过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}