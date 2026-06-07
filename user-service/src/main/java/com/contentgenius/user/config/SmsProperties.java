package com.contentgenius.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 号码认证 / 短信认证配置（SendSmsVerifyCode）。
 * <p>
 * Nacos 示例（控制台「快速测试」里的系统签名与模板）：
 * <pre>
 * contentgenius:
 *   sms:
 *     access-key-id: xxx
 *     access-key-secret: xxx
 *     sign-name: 速通互联验证码
 *     template-code: "100001"
 *     code-ttl-seconds: 300
 *     send-interval-seconds: 60
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "contentgenius.sms")
public class SmsProperties {

    /** 阿里云 AccessKey ID */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret */
    private String accessKeySecret;

    /** 系统赠送签名，如：速通互联验证码 */
    private String signName;

    /** 系统赠送模板 Code，如：100001 */
    private String templateCode;

    /** 验证码有效期（秒），对应模板变量 min（分钟）与接口 validTime */
    private int codeTtlSeconds = 300;

    /** 同一手机号最短发送间隔（秒），对应接口 interval */
    private int sendIntervalSeconds = 60;

    public boolean isAliyunConfigured() {
        return StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret)
                && StringUtils.hasText(signName)
                && StringUtils.hasText(templateCode);
    }

    /** 模板变量 min：验证码有效分钟数 */
    public int templateMinMinutes() {
        return Math.max(1, codeTtlSeconds / 60);
    }
}

