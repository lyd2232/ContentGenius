package com.contentgenius.agent.service.serviceimpl;

import com.contentgenius.agent.service.RagQueryService;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * RAG 检索实现（Qdrant 等），计划 6/17 接入。
 * <p>6/9 写稿请走 {@link com.contentgenius.agent.service.impl.AgentChatServiceImpl}，不要写在本类。
 */
@Service
public class RagQueryServiceimpl implements RagQueryService {

    @Override
    public String query(String question) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "RAG 检索尚未接入，请使用 POST /api/agent/chat 生成草稿");
    }
}
