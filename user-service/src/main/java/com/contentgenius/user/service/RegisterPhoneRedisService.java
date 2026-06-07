package com.contentgenius.user.service;

import com.contentgenius.user.util.PhoneUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class RegisterPhoneRedisService {

    private static final String KEY_PREFIX = "register:phone:";

    private final StringRedisTemplate redisTemplate;

    public RegisterPhoneRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public boolean isPhoneRegistered(String phone) {
        String key = buildKey(phone);
        if (key == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 注册成功后调用：写入手机号与注册时间。
     */
    public void saveRegisterTime(String phone) {
        String key = buildKey(phone);
        if (key == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
    }

    private String buildKey(String phone) {
        String normalized = PhoneUtils.normalize(phone);
        if (normalized == null) {
            return null;
        }
        return KEY_PREFIX + normalized;
    }
}
