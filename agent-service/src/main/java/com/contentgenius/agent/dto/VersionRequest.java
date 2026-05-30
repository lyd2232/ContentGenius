package com.contentgenius.agent.dto;

import lombok.Data;

/**
 * 与 content-service {@code CreateContentVersionRequest} 字段对齐，供 Feign 存版本用。
 */
@Data
public class VersionRequest {

    private String title;

    private String content;

    private String platform;

    /** agent / manual / import */
    private String source;
}
