package com.contentgenius.agent.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 Tavily 联网摘要中的真实 URL 追加到成稿末尾，避免依赖模型自觉输出「参考链接」。
 */
public final class WebReferenceAppender {

    private static final Pattern HTTP_URL = Pattern.compile("https?://\\S+");
    private static final int MAX_LINKS = 5;

    private WebReferenceAppender() {
    }

    public static String appendReferences(String content, String webContext) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(webContext)) {
            return content;
        }
        if (hasReferenceSection(content)) {
            return content;
        }
        List<String> urls = extractUrls(webContext);
        if (urls.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder(content.stripTrailing());
        sb.append("\n\n参考链接\n");
        for (String url : urls) {
            sb.append(url).append('\n');
        }
        return sb.toString();
    }

    private static boolean hasReferenceSection(String content) {
        int idx = content.lastIndexOf("参考链接");
        if (idx < 0) {
            return false;
        }
        return HTTP_URL.matcher(content.substring(idx)).find();
    }

    private static List<String> extractUrls(String webContext) {
        Set<String> unique = new LinkedHashSet<>();
        Matcher matcher = HTTP_URL.matcher(webContext);
        while (matcher.find()) {
            String url = matcher.group().replaceAll("[)\\],;.!?\"'》】]+$", "");
            unique.add(url);
            if (unique.size() >= MAX_LINKS) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }
}
