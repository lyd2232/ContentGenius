package com.contentgenius.content.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateContentVersionRequest {

    @Size(max = 256, message = "标题最多 256 个字符")
    private String title;

    private String content;

    @Size(max = 32, message = "平台标识最多 32 个字符")
    private String platform;

    @Size(max = 32, message = "来源最多 32 个字符")
    private String source;
}
