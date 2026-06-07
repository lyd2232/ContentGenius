package com.contentgenius.user.util;

import org.springframework.util.StringUtils;
//验证码生成工具
public final class PhoneUtils {

    private static final String CN_MOBILE_PATTERN = "^1[3-9]\\d{9}$";

    private PhoneUtils() {
    }

    public static String normalize(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.trim().replaceAll("\\s+", "");
    }

    public static boolean isValidCnMobile(String phone) {
        String normalized = normalize(phone);
        return normalized != null && normalized.matches(CN_MOBILE_PATTERN);
    }
}
