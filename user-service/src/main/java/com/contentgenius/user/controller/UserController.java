package com.contentgenius.user.controller;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.service.LoginService;
import com.contentgenius.user.service.RegisterService;
import com.contentgenius.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Map<String, Boolean>> register(@RequestBody RegisterRequest request) {
        Boolean success = registerService.register(new RegisterService.RegisterParam(
                request.username(),
                request.password(),
                request.email(),
                request.phone()
        ));
        return Result.ok(Map.of("success", success));
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = loginService.login(request.username(), request.password());
        return Result.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Object>> me() {
        User user = currentUser();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("phone", user.getPhone());
        body.put("memberLevel", user.getMemberLevel());
        return Result.ok(body);
    }

    @PutMapping("/me1")
    @PreAuthorize("hasAuthority('user:write')")
    public Result<Map<String, Boolean>> updateMe(@RequestBody UpdateProfileRequest request) {
        User user = currentUser();
        User patch = new User();
        patch.setId(user.getId());
        patch.setEmail(request.email());
        patch.setPhone(request.phone());
        patch.setPassword(request.password());
        Boolean success = userService.update(patch);
        return Result.ok(Map.of("success", success));
    }

    private User currentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public record RegisterRequest(String username, String password, String email, String phone) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record UpdateProfileRequest(String email, String phone, String password) {
    }
}
