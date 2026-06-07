package com.contentgenius.user.service.serviceimpl;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.service.RegisterPhoneRedisService;
import com.contentgenius.user.service.RegisterService;
import com.contentgenius.user.service.SmsCodeService;
import com.contentgenius.user.service.UserService;
import com.contentgenius.user.util.PhoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegisterServiceimpl implements RegisterService {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisterPhoneRedisService registerPhoneRedisService;

    @Autowired
    private SmsCodeService smsCodeService;

    @Override
    public Boolean register(RegisterParam param) {
        if (param == null || !StringUtils.hasText(param.username()) || !StringUtils.hasText(param.password())) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_REQUIRED);
        }
        if (!StringUtils.hasText(param.phone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写手机号");
        }
        if (!PhoneUtils.isValidCnMobile(param.phone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
        if (userService.findByUsername(param.username()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        String phone = PhoneUtils.normalize(param.phone());
        // 读redis是否存在
        if (registerPhoneRedisService.isPhoneRegistered(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该手机号已注册");
        }
        // 校验验证码
        smsCodeService.verifyAndConsume(phone, param.smsCode());

        User user = new User();
        user.setUsername(param.username());
        user.setPassword(passwordEncoder.encode(param.password()));
        user.setEmail(param.email());
        user.setPhone(phone);
        // 分配额度
        user.setMemberLevel(2);
        user.setStatus(1);

        Boolean saved = userService.save(user);
        if (Boolean.TRUE.equals(saved)) {
            // 【存】注册成功后登记手机号
            registerPhoneRedisService.saveRegisterTime(phone);
        }
        return saved;
    }
}
