package com.contentgenius.user.service;

import com.contentgenius.user.entity.User;

public interface UserService {

    User findByUsername(String username);

    User findById(Long id);

    Boolean save(User user);

    /**
     * 更新资料：仅允许改 email、phone、password（明文入参会 BCrypt）。
     * 不改 username、memberLevel、status。
     */
    Boolean update(User user);

    Boolean delete(Long id);

    /** 校验当前密码后修改为新密码（明文入参） */
    Boolean changePassword(Long userId, String oldPassword, String newPassword);

    /** 校验密码后注销账号（物理删除） */
    Boolean deleteAccount(Long userId, String password);
}
