package com.contentgenius.user.service;

/**
 * 注册入参：仅允许这 4 个字段，避免客户端传入 id、status、memberLevel 等。
 */
public interface RegisterService {

    Boolean register(RegisterParam param);

    record RegisterParam(String username, String password, String email, String phone) {
    }
}
