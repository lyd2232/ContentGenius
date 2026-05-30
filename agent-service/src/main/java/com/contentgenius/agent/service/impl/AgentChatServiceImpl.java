package com.contentgenius.agent.service.impl;

import com.contentgenius.agent.core.ContentGeniusAgent;
import com.contentgenius.agent.dto.AgentChatRequest;
import com.contentgenius.agent.dto.AgentChatResponse;
import com.contentgenius.agent.service.AgentChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        return contentGeniusAgent.chat(
                request.getProjectId(), request.getTopic(), request.getPlatform());
    }
}
