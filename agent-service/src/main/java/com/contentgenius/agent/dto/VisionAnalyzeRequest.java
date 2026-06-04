package com.contentgenius.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 图生文风格解析：上传后由前端传入 MinIO objectName。
 */
@Data
public class VisionAnalyzeRequest {

    @NotBlank(message = "objectName 不能为空")
    private String objectName;

    /** 可选：xiaohongshu / wechat 等，用于提示模型对照平台语气 */
    private String platform;
}
