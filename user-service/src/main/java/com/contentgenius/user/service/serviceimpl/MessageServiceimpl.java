package com.contentgenius.user.service.serviceimpl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import com.contentgenius.user.config.SmsProperties;
import com.contentgenius.user.service.MessageService;
import com.contentgenius.user.util.PhoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 阿里云号码认证底层发送：调用 {@code SendSmsVerifyCode}。
 * <p>
 * 使用控制台「快速测试」配套的系统签名 + 模板（如 速通互联验证码 / 100001）。
 */
@Slf4j
@Service
public class MessageServiceimpl implements MessageService {

    private final Client aliyunDypnsClient;
    private final SmsProperties smsProperties;

    public MessageServiceimpl(@Autowired(required = false) Client aliyunDypnsClient,
                              SmsProperties smsProperties) {
        this.aliyunDypnsClient = aliyunDypnsClient;
        this.smsProperties = smsProperties;
    }

    /**
     * 调用 SendSmsVerifyCode，模板变量 {@code {"code":"123456","min":"5"}}。
     */
    @Override
    public Result<Void> sendMessage(String phoneNumber, String code) {
        if (!PhoneUtils.isValidCnMobile(phoneNumber)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码不能为空");
        }
        if (aliyunDypnsClient == null || !smsProperties.isAliyunConfigured()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "短信服务未配置，请检查 contentgenius.sms");
        }

        String phone = PhoneUtils.normalize(phoneNumber);
        int min = smsProperties.templateMinMinutes();
        // 登录/注册模板 100001 需要 code + min 两个变量
        String templateParam = String.format("{\"code\":\"%s\",\"min\":\"%d\"}", code, min);

        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName(smsProperties.getSignName())
                .setTemplateCode(smsProperties.getTemplateCode())
                .setTemplateParam(templateParam)
                .setValidTime((long) smsProperties.getCodeTtlSeconds())
                .setInterval((long) smsProperties.getSendIntervalSeconds());

        try {
            SendSmsVerifyCodeResponse response =
                    aliyunDypnsClient.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            String bizCode = response.getBody() != null ? response.getBody().getCode() : null;
            if (!"OK".equalsIgnoreCase(bizCode)) {
                String bizMessage = response.getBody() != null ? response.getBody().getMessage() : "未知错误";
                log.warn("号码认证发短信失败 phone={} code={} message={}", maskPhone(phone), bizCode, bizMessage);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "短信发送失败：" + bizMessage);
            }
            log.info("号码认证短信已发送 phone={}", maskPhone(phone));
            return Result.ok();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("号码认证短信调用异常 phone={}", maskPhone(phone), ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "短信发送失败，请稍后重试");
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
