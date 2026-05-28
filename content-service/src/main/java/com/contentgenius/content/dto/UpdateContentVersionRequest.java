package com.contentgenius.content.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateContentVersionRequest {

    @Size(max = 256, message = "标题最多 256 个字符")
    private String title;

    private String content;

    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;

    /** 0草稿 1已定稿 */
    private Integer status;
}
