package com.contentgenius.user.service.serviceimpl;

import com.contentgenius.user.entity.User;
import com.contentgenius.user.service.LoginService;
import com.contentgenius.user.service.UserService;
import com.contentgenius.user.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class LoginServiceimpl implements LoginService {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public String login(String username, String password) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return jwtUtils.createToken(username);
    }
}