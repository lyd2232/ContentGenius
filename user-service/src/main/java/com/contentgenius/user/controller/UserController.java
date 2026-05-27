package com.contentgenius.user.controller;

import com.contentgenius.user.service.LoginService;
import com.contentgenius.user.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private LoginService loginService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        Boolean success = registerService.register(new RegisterService.RegisterParam(
                request.username(),
                request.password(),
                request.email(),
                request.phone()
        ));
        return Map.of("success", success);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        String token = loginService.login(request.username(), request.password());
        return Map.of("token", token);
    }

    /** 需登录，用于验收 JWT 过滤器 */

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('content:write')")

    public Map<String, String> me() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Map.of("username", username);
    }

    public record RegisterRequest(String username, String password, String email, String phone) {
    }

    public record LoginRequest(String username, String password) {
    }
}
