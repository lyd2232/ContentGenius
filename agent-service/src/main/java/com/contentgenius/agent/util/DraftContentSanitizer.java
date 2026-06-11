package com.contentgenius.agent.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 清理稿件末尾无效的「参考链接」占位内容（模型无真实 URL 时常见）。
 */
public final class DraftContentSanitizer {

    private static final Pattern HTTP_URL = Pattern.compile("https?://\\S+");
    private static final Pattern PLACEHOLDER_LINK = Pattern.compile(
            "\\[链接\\d*]|这里应该放置|真实存在的链接|占位|应放置.*链接",
            Pattern.CASE_INSENSITIVE);

    private DraftContentSanitizer() {
    }

    public static String sanitize(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        int idx = content.lastIndexOf("参考链接");
        if (idx < 0) {
            return content;
        }
        String tail = content.substring(idx);
        if (HTTP_URL.matcher(tail).find() && !PLACEHOLDER_LINK.matcher(tail).find()) {
            return content;
        }
        return content.substring(0, idx).stripTrailing();
    }
}
