package com.contentgenius.content.dto;

import lombok.Data;

/** 与 agent-service {@code RagSimilarityHit} 字段对齐，供 Feign 反序列化 */
@Data
public class RagSimilarityHit {

    private Long versionId;

    private Double score;

    private String snippet;
}
