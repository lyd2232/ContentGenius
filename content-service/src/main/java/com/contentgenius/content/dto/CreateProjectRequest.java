package com.contentgenius.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最多 128 个字符")
    private String title;

    @Size(max = 512, message = "描述最多 512 个字符")
    private String description;
}
