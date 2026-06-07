package com.contentgenius.user.service;

import com.contentgenius.user.config.SmsProperties;
import com.contentgenius.user.util.PhoneUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码在 Redis 中的读写（与「已注册手机号」{@link RegisterPhoneRedisService} 分开存）。
 * <p>
 * sms:code:{手机号} → 验证码正文；sms:lock:{手机号} → 发码频控占位。
 */
@Service
public class SmsCodeRedisService {

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String LOCK_KEY_PREFIX = "sms:lock:";

    private final StringRedisTemplate redisTemplate;
    private final SmsProperties smsProperties;

    public SmsCodeRedisService(StringRedisTemplate redisTemplate, SmsProperties smsProperties) {
        this.redisTemplate = redisTemplate;
        this.smsProperties = smsProperties;
    }

    /**
     * 注册
     */
    public String getCode(String phone) {
        String key = codeKey(phone);
        if (key == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 【存方法】发码成功后：写入验证码
     */
    public void saveCode(String phone, String code) {
        String key = codeKey(phone);
        if (key == null) {
            return;
        }
        redisTemplate.opsForValue().set(
                key,
                code,
                smsProperties.getCodeTtlSeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * 【删方法】校验通过后：删除验证码，防止重复使用。
     */
    public void deleteCode(String phone) {
        String key = codeKey(phone);
        if (key == null) {
            return;
        }
        redisTemplate.delete(key);
    }

    /**
     * 【读方法】发码前：是否处于发送冷却期（60 秒内已发过）。
     */
    public boolean isSendLocked(String phone) {
        String key = lockKey(phone);
        if (key == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 【存方法】发码成功后：记录发送锁
     */
    public void lockSend(String phone) {
        String key = lockKey(phone);
        if (key == null) {
            return;
        }
        redisTemplate.opsForValue().set(
                key,
                "1",
                smsProperties.getSendIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    private String codeKey(String phone) {
        String normalized = PhoneUtils.normalize(phone);
        if (normalized == null) {
            return null;
        }
        return CODE_KEY_PREFIX + normalized;
    }

    private String lockKey(String phone) {
        String normalized = PhoneUtils.normalize(phone);
        if (normalized == null) {
            return null;
        }
        return LOCK_KEY_PREFIX + normalized;
    }
}
