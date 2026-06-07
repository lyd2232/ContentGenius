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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
                request.phone(),
                request.smsCode()
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

    /** 更新邮箱、手机号（不改用户名与密码） */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Boolean>> updateMe(@RequestBody UpdateProfileRequest request) {
        User user = currentUser();
        User patch = new User();
        patch.setId(user.getId());
        if (request.email() != null) {
            patch.setEmail(request.email().trim());
        }
        if (request.phone() != null) {
            patch.setPhone(request.phone().trim());
        }
        Boolean success = userService.update(patch);
        return Result.ok(Map.of("success", success));
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Boolean>> changePassword(@RequestBody ChangePasswordRequest request) {
        if (!StringUtils.hasText(request.oldPassword()) || !StringUtils.hasText(request.newPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写当前密码和新密码");
        }
        User user = currentUser();
        Boolean success = userService.changePassword(user.getId(), request.oldPassword(), request.newPassword());
        return Result.ok(Map.of("success", success));
    }

    /** 注销账号：校验密码后删除用户记录 */
    @DeleteMapping("/me")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Boolean>> deleteMe(@RequestBody DeleteAccountRequest request) {
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入密码以确认注销");
        }
        User user = currentUser();
        Boolean success = userService.deleteAccount(user.getId(), request.password());
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

    public record RegisterRequest(String username, String password, String email, String phone, String smsCode) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record UpdateProfileRequest(String email, String phone) {
    }

    public record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    public record DeleteAccountRequest(String password) {
    }
}
