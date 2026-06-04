package com.contentgenius.agent.writer;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

//模型路由
@Service
public class SmartRouter {

    private static final String INTENT_INSTRUCTION =
            "判断写作意图，只回复一个词：outline|rewrite|style|title。"
            + "outline=从零写；rewrite=已有稿整篇重写；style=润色；title=只改标题。";

    private final ChatModel intentChatModel;

    public SmartRouter(@Qualifier("qwenMaxChatModel") ChatModel intentChatModel) {
        this.intentChatModel = intentChatModel;
    }
    // 根据用户输入，判断写作意图
    public WriteIntent resolveIntent(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return WriteIntent.OUTLINE;
        }
        String raw = intentChatModel.chat(INTENT_INSTRUCTION + "\n用户输入：" + userMessage.trim());
        return parseIntent(raw);
    }
    // 解析意图转为枚举
    static WriteIntent parseIntent(String raw) {
        if (!StringUtils.hasText(raw)) {
            return WriteIntent.OUTLINE;
        }
        String n = raw.trim().toLowerCase();
        if (n.contains("title") || n.contains("标题")) {
            return WriteIntent.TITLE;
        }
        if (n.contains("style") || n.contains("润色")) {
            return WriteIntent.STYLE;
        }
        if (n.contains("rewrite") || n.contains("重写") || n.contains("改写")) {
            return WriteIntent.REWRITE;
        }
        if (n.contains("outline") || n.contains("大纲")) {
            return WriteIntent.OUTLINE;
        }
        return WriteIntent.OUTLINE;
    }

    public enum WriteIntent {
        OUTLINE,
        REWRITE,
        STYLE,
        TITLE
    }
}
