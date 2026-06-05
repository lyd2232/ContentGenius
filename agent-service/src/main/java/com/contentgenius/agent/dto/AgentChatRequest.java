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


    @NotNull(message = "项目 id 不能为空")
    private Long projectId;



    @NotBlank(message = "创作主题不能为空")
    @Size(max = 512, message = "创作主题最多 512 个字符")
    private String creationTheme;


    @NotBlank(message = "本轮指令不能为空")
    @Size(max = 512, message = "本轮指令最多 512 个字符")
    private String topic;

    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;

    Boolean isopen;//

    Boolean useRag;


    private String mode;

    //多轮改稿的标志性id
    @Size(max = 64, message = "sessionId 最多 64 个字符")
    private String sessionId;

    /** 多轮记忆 id，由前端传入 */
    private Integer memoryId;

    //显性注入路由
    @Size(max = 16, message = "thinkAction 最多 16 个字符")
    private String thinkAction;
}
