package com.contentgenius.agent.model;

//前端传参写稿模式
public enum ChatMode {

    /** 快速模式：一次 write，低延迟 */
    FAST,
    /** 思考模式：意图识别 + outline/write/style/title 等编排 */
    THINK;

    public static ChatMode from(String value) {
        if (value == null || value.isBlank()) {
            return FAST;
        }
        if ("think".equalsIgnoreCase(value.trim())) {
            return THINK;
        }
        return FAST;
    }

    public String getCode() {
        return this == THINK ? "think" : "fast";
    }
}
