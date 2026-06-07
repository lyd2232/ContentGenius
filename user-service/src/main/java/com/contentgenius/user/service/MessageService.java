package com.contentgenius.user.service;

import com.contentgenius.common.result.Result;

/**
 * 阿里云号码认证底层接口（由 {@link com.contentgenius.user.service.SmsCodeService} 编排发码流程）。
 */
public interface MessageService {
    /**
     * 调用 SendSmsVerifyCode，将验证码发到手机。
     *
     * @param phoneNumber 手机号
     * @param code        验证码，对应模板变量 code；min 由配置自动计算
     */

    Result<Void> sendMessage(String phoneNumber, String code);
}
