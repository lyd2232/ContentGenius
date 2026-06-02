package com.contentgenius.common.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定稿后写入 Redis 队列、供 agent 异步入 Qdrant 的任务载荷。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagIndexJob {

    public static final String QUEUE_KEY = "rag:index:queue";

    private Long versionId;

    private Long userId;

    private String platform;

    private String content;
}
