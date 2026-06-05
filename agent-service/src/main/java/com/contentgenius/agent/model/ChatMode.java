package com.contentgenius.agent.model;

//前端传参写稿模式
public enum ChatMode {

    /** 快速模式：一次 write，低延迟 */
    FAST,
    /** 思考模式：显式 thinkAction + outline/write/style/title 编排 */
    THINK,
    /** 智能路由：SmartRouter 判意图 + 同思考模式编排（演示用，准确性待优化） */
    SMART;

    public static ChatMode from(String value) {
        if (value == null || value.isBlank()) {
            return FAST;
        }
        String v = value.trim();
        if ("think".equalsIgnoreCase(v)) {
            return THINK;
        }
        if ("smart".equalsIgnoreCase(v)) {
            return SMART;
        }
        return FAST;
    }

    public String getCode() {
        return switch (this) {
            case THINK -> "think";
            case SMART -> "smart";
            default -> "fast";
        };
    }

    /** 走同步四步 / SmartRouter 编排，非快速单次 write */
    public boolean usesThinkPipeline() {
        return this == THINK || this == SMART;
    }
}
