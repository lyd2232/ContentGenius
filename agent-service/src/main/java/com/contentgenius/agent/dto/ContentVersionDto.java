package com.contentgenius.agent.dto;

import lombok.Data;

/**
 * 与 content-service 返回的 content_version JSON 对齐（Feign 反序列化用）。
 */
@Data
public class ContentVersionDto {

    private Long id;

    private Long projectId;

    private Integer versionNo;

    private String title;

    private String content;

    private String platform;

    private String source;

    private Integer status;
}
