package com.contentgenius.agent.tools;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 带联网搜索工具的助手接口。
 */
public interface ChatAssistant {

    @UserMessage("""
            你必须先调用联网搜索工具，再回答。
            请围绕主题做实时信息检索，输出：
            1) 3-5 条关键事实（尽量含时间与数据）；
            2) 至少 3 个可访问来源链接（原始 URL）；
            3) 最后给一段 120 字以内结论。
            主题：{{message}}
            """)
    String chat(@V("message") String message);
}