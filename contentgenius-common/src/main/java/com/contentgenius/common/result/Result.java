package com.contentgenius.common.result;

import com.contentgenius.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
//状态码
    private int code;
    //返回信息
    private String message;
    //数据
    private T data;
//成功返回
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }
//无参成功返回
    public static <T> Result<T> ok() {
        return ok(null);
    }
//失败返回
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
//失败返回带描述
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
//失败返回自定义描述
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
