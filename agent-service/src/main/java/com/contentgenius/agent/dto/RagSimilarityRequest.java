package com.contentgenius.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RagSimilarityRequest {

    @NotNull(message = "版本 id 不能为空")
    private Long versionId;

    @NotBlank(message = "正文不能为空")
    private String content;

    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;
}
