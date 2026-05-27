package com.contentgenius.user.config;

import com.contentgenius.common.util.JwtUtils;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.service.PermissionService;
import com.contentgenius.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    private List<String> skipAuthUrls;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String url = request.getRequestURI();
        if (skipAuthUrls != null && isSkipUrl(url)) {
            chain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if (!StringUtils.hasText(auth)) {
            unauthorized(response);
            return;
        }

        String token = auth.startsWith("Bearer ") ? auth.substring(7).trim() : auth.trim();
        if (!jwtUtils.verify(token)) {
            unauthorized(response);
            return;
        }

        String username = jwtUtils.getUsername(token);
        User user = userService.findByUsername(username);
        if (user == null) {
            unauthorized(response);
            return;
        }

        List<SimpleGrantedAuthority> authorities = permissionService
                .listCodesForUser(user.getId(), user.getMemberLevel())
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    public boolean isSkipUrl(String url) {
        for (String skipAuthUrl : skipAuthUrls) {
            if (url.startsWith(skipAuthUrl)) {
                return true;
            }
        }
        return false;
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        byte[] body = "{\"code\":401,\"message\":\"未登录或 token 无效\"}".getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(body);
    }
}
