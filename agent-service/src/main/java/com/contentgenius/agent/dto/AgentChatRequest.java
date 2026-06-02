package com.contentgenius.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/agent/chat 的请求体（JSON）。
 */
@Data
public class AgentChatRequest {

    /** 存稿目标项目，来自 content-service 建项目或列表接口返回的 id */
    @NotNull(message = "项目 id 不能为空")
    private Long projectId;

    /** 用户本次要写什么 */
    @NotBlank(message = "创作主题不能为空")
    @Size(max = 512, message = "主题最多 512 个字符")
    private String topic;

    /**
     * 发布平台，用于查 template 表并拼 prompt_hint。
     * 常见值：xiaohongshu、wechat；不传时业务层默认 xiaohongshu。
     */
    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;

    Boolean isopen;
}
