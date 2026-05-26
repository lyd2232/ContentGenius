package com.contentgenius.user.service.serviceimpl;

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
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if (userService.findByUsername(param.username()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(param.username());
        //密码加密
        user.setPassword(passwordEncoder.encode(param.password()));
        user.setEmail(param.email());
        user.setPhone(param.phone());
        user.setMemberLevel(0);
        user.setStatus(1);

        return userService.save(user);
    }
}
