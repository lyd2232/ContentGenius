package com.contentgenius.agent.service;

/**
 * RAG 向量检索（6/17+），与写稿接口 {@link AgentChatService} 分开。
 */
public interface RagQueryService {

    String query(String question);
}
