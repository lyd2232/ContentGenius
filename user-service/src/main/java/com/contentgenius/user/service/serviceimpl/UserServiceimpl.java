package com.contentgenius.user.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.mapper.UserMapper;
import com.contentgenius.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceimpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public Boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public Boolean update(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
        }
        User db = userMapper.selectById(user.getId());
        if (db == null) {
            return false;
        }
        if (StringUtils.hasText(user.getEmail())) {
            db.setEmail(user.getEmail());
        }
        if (StringUtils.hasText(user.getPhone())) {
            db.setPhone(user.getPhone());
        }
        if (StringUtils.hasText(user.getPassword())) {
            db.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.updateById(db) > 0;
    }

    @Override
    public Boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }
}
