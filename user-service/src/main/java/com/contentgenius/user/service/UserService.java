package com.contentgenius.user.service;

import com.contentgenius.user.entity.User;

public interface UserService {
    User findByUsername(String username);//登录
    Boolean save(User user);//注册
    Boolean update(User user);//修改
    Boolean delete(Long id);//删除
}
