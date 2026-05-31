package com.contentgenius.agent.service;

import com.contentgenius.agent.dto.AgentChatRequest;
import com.contentgenius.agent.dto.AgentChatResponse;
import reactor.core.publisher.Flux;

/**
 * 写稿业务接口使用ai助手
 */
public interface AgentChatService {

    /**
     * @param request 含 projectId、topic、platform
     * @return 生成草稿与平台
     */
    AgentChatResponse chat(AgentChatRequest request);
    Flux<AgentChatResponse> chatStream(AgentChatRequest request);
}
