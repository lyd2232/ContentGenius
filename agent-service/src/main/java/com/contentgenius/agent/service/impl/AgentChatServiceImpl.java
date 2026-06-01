package com.contentgenius.agent.service.impl;

import com.contentgenius.agent.core.ContentGeniusAgent;
import com.contentgenius.agent.dto.AgentChatRequest;
import com.contentgenius.agent.dto.AgentChatResponse;
import com.contentgenius.agent.service.AgentChatService;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 写稿 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AgentChatServiceImpl implements AgentChatService {

    private final ContentGeniusAgent contentGeniusAgent;

    /**
     *
     * @param request 含 topic、platform//前端传来的数据写什么
     * @return
     */
    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        String topic = request.getTopic();
        if (SensitiveWordHelper.contains(topic)) {//判断有没有敏感词
            String replacedText = SensitiveWordHelper.replace(topic);//替换敏感词
            throw new BusinessException(
                    ErrorCode.CONTENT_CONTAINS_SENSITIVE_WORDS,
                    "内容包含敏感词，请修改后重试。参考：" + replacedText
            );
        }
        return contentGeniusAgent.chat(
                request.getProjectId(), topic, request.getPlatform());
    }

    @Override
    public Flux<AgentChatResponse> chatStream(AgentChatRequest request) {
        String topic = request.getTopic();
        if (SensitiveWordHelper.contains(topic)) {//判断有没有敏感词
            String replacedText = SensitiveWordHelper.replace(topic);//替换敏感词
            throw new BusinessException(
                    ErrorCode.CONTENT_CONTAINS_SENSITIVE_WORDS,
                    "内容包含敏感词，请修改后重试。参考：" + replacedText
            );
        }
        return contentGeniusAgent.streamChat(
                request.getProjectId(), request.getTopic(), request.getPlatform());
    }
}
