package com.contentgenius.common.exception;

import lombok.Getter;

@Getter
/**
 * 业务异常类
 * 用于封装业务处理过程中的错误信息，包含错误码和错误消息
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 使用错误码构造业务异常
     *
     * @param errorCode 错误码枚举对象，从中获取错误码和错误消息
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码和自定义消息构造业务异常
     *
     * @param errorCode 错误码枚举对象，从中获取错误码
     * @param message 自定义的错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用指定的错误码和消息构造业务异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
