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
        if (user.getEmail() != null) {
            db.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
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

    @Override
    public Boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User db = requireUser(userId);
        assertPasswordMatches(db, oldPassword);
        assertNewPasswordValid(newPassword);
        db.setPassword(passwordEncoder.encode(newPassword.trim()));
        return userMapper.updateById(db) > 0;
    }

    @Override
    public Boolean deleteAccount(Long userId, String password) {
        User db = requireUser(userId);
        assertPasswordMatches(db, password);
        return userMapper.deleteById(userId) > 0;
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
        }
        User db = userMapper.selectById(userId);
        if (db == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return db;
    }

    private void assertPasswordMatches(User db, String rawPassword) {
        if (!StringUtils.hasText(rawPassword) || !passwordEncoder.matches(rawPassword, db.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "密码不正确");
        }
    }

    private static void assertNewPasswordValid(String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.trim().length() < 6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码至少 6 位");
        }
    }
}
