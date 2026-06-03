package com.contentgenius.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手动写入 Qdrant 向量（调试 / 补入库）。
 */
@Data
public class RagIngestRequest {

    @NotNull(message = "版本 id 不能为空")
    private Long versionId;

    @NotBlank(message = "正文不能为空")
    private String content;

    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;
}
