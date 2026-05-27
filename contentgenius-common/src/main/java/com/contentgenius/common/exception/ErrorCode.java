package com.contentgenius.common.exception;

import lombok.Getter;
//定义的一些固定返回格式
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求参数错误"),
    USER_NOT_FOUND(40001, "用户不存在"),
    INVALID_CREDENTIALS(40002, "用户名或密码错误"),
    USERNAME_EXISTS(40003, "用户名已存在"),
    USERNAME_PASSWORD_REQUIRED(40004, "用户名和密码不能为空"),
    USER_ID_REQUIRED(40005, "用户 id 不能为空"),
    FORBIDDEN(40300, "无访问权限"),
    NOT_FOUND(40400, "请求路径不存在"),
    BAD_GATEWAY(50200, "下游服务异常"),
    SERVICE_UNAVAILABLE(50300, "服务暂时不可用"),
    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
