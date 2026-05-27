package com.contentgenius.user.service.serviceimpl;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.service.RegisterService;
import com.contentgenius.user.service.UserService;
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

    @Override
    public Boolean register(RegisterParam param) {
        if (param == null || !StringUtils.hasText(param.username()) || !StringUtils.hasText(param.password())) {
            throw new BusinessException(ErrorCode.USERNAME_PASSWORD_REQUIRED);
        }
        if (userService.findByUsername(param.username()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(param.username());
        user.setPassword(passwordEncoder.encode(param.password()));
        user.setEmail(param.email());
        user.setPhone(param.phone());
        user.setMemberLevel(0);
        user.setStatus(1);

        return userService.save(user);
    }
}
