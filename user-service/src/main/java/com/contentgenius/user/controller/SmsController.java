package com.contentgenius.user.controller;

import com.contentgenius.common.result.Result;
import com.contentgenius.user.service.SmsCodeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 短信验证码接口（注册发码，匿名可访问）。
 */
@RestController
@RequestMapping("/api/users/sms")
public class SmsController {

    private final SmsCodeService smsCodeService;

    public SmsController(SmsCodeService smsCodeService) {
        this.smsCodeService = smsCodeService;
    }

    /**
     * 发送注册验证码：生成码 → 存 Redis → 阿里云 SendSmsVerifyCode。
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendRegisterCode(@RequestBody SendSmsRequest request) {
        Map<String, Object> data = smsCodeService.sendRegisterCode(request.phone());
        return Result.ok(data);
    }

    public record SendSmsRequest(String phone) {
    }
}
