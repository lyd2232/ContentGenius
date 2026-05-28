package com.contentgenius.content.config;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;

//获取当前用户id免得每次都传用户
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long getUserId() {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "未登录");
    }
}
