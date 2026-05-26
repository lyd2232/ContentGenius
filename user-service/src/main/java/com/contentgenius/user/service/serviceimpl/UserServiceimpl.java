package com.contentgenius.user.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.mapper.UserMapper;
import com.contentgenius.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceimpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {

        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    @Override
    public Boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public Boolean update(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    public Boolean delete(Long id) {
        return userMapper.deleteById(id) > 0;
    }
}
