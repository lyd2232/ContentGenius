package com.contentgenius.user.service;

import java.util.Map;

/**
 * 注册短信验证码：生成、发阿里云、Redis 存取与校验。
 */
public interface SmsCodeService {

    /**
     * 向手机号发送注册验证码（生成 6 位数字 → 存 Redis → 调阿里云发短信）。
     */
    Map<String, Object> sendRegisterCode(String phone);

    /**
     * 注册提交时校验验证码；正确则删除 Redis 中的码（一次性）。
     */
    void verifyAndConsume(String phone, String smsCode);
}
