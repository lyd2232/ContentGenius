package com.contentgenius.content.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectRequest {

    @Size(max = 128, message = "标题最多 128 个字符")
    private String title;

    @Size(max = 512, message = "描述最多 512 个字符")
    private String description;

    /** 1进行中 2已归档 */
    private Integer status;
}
