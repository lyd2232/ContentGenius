package com.contentgenius.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST /api/agent/chat 的业务数据（包在 Result.data 里返回）。
 */
@Data
@NoArgsConstructor

public class AgentChatResponse {

    /** 大模型生成的完整草稿（含标题 + 正文） */
    private String content;

    /** 实际使用的平台 */
    private String platform;

    /** 写入 content_version 后的版本 id */
    private Long versionId;

    /** 同一 project 下的版本序号 */
    private Integer versionNo;

    /** 本次使用的模式：fast | think */
    private String mode;


    private Integer memoryId;

    public AgentChatResponse(String content, String platform, Long versionId, Integer versionNo) {
        this.content = content;
        this.platform = platform;
        this.versionId = versionId;
        this.versionNo = versionNo;
    }

    public AgentChatResponse(String content, String platform, Long versionId, Integer versionNo, String mode) {
        this.content = content;
        this.platform = platform;
        this.versionId = versionId;
        this.versionNo = versionNo;
        this.mode = mode;
    }
}
