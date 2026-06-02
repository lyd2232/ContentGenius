package com.contentgenius.common.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RAG 入队消息的 JSON 序列化（content 生产、agent 消费共用）。
 */
public final class RagIndexJobSerde {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RagIndexJobSerde() {
    }

    public static String toJson(RagIndexJob job) {
        try {
            return MAPPER.writeValueAsString(job);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("RAG 入队任务序列化失败", e);
        }
    }

    public static RagIndexJob fromJson(String json) {
        try {
            return MAPPER.readValue(json, RagIndexJob.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("RAG 入队任务反序列化失败: " + json, e);
        }
    }
}
