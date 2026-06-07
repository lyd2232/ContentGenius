package com.contentgenius.user.service.serviceimpl;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.user.service.MessageService;
import com.contentgenius.user.service.RegisterPhoneRedisService;
import com.contentgenius.user.service.SmsCodeRedisService;
import com.contentgenius.user.service.SmsCodeService;
import com.contentgenius.user.util.PhoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Map;

@Service
public class SmsCodeServiceimpl implements SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsCodeRedisService smsCodeRedisService;
    private final RegisterPhoneRedisService registerPhoneRedisService;
    private final MessageService messageService;

    public SmsCodeServiceimpl(SmsCodeRedisService smsCodeRedisService,
                              RegisterPhoneRedisService registerPhoneRedisService,
                              MessageService messageService) {
        this.smsCodeRedisService = smsCodeRedisService;
        this.registerPhoneRedisService = registerPhoneRedisService;
        this.messageService = messageService;
    }

    @Override
    public Map<String, Object> sendRegisterCode(String phone) {
        if (!PhoneUtils.isValidCnMobile(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
        String normalized = PhoneUtils.normalize(phone);

        // 读redis校验是否存在
        if (registerPhoneRedisService.isPhoneRegistered(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该手机号已注册");
        }
        // 校验是否重复刷
        if (smsCodeRedisService.isSendLocked(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发送太频繁，请稍后再试");
        }

        String code = generateSixDigitCode();

        // 存redis设置验证码过期时间
        smsCodeRedisService.saveCode(normalized, code);
        //存redis60秒内不可重复刷
        smsCodeRedisService.lockSend(normalized);

        // 调阿里云号码认证 SendSmsVerifyCode
        messageService.sendMessage(normalized, code);

        return Map.of("sent", true);
    }

    @Override
    public void verifyAndConsume(String phone, String smsCode) {
        if (!StringUtils.hasText(smsCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写短信验证码");
        }
        String normalized = PhoneUtils.normalize(phone);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }

        // 【读】取出 Redis 中的验证码
        String cached = smsCodeRedisService.getCode(normalized);
        if (!StringUtils.hasText(cached)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!cached.equals(smsCode.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
        // 【删】校验通过，一次性消费
        smsCodeRedisService.deleteCode(normalized);
    }

    /** 生成 6 位数字验证码 */
    private static String generateSixDigitCode() {
        int value = RANDOM.nextInt(900_000) + 100_000;
        return String.valueOf(value);
    }
}
