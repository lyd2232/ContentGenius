package com.contentgenius.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高相似历史稿命中结果（定稿去重提示、Feign 返回用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagSimilarityHit {

    /** 历史稿 content_version.id */
    private Long versionId;

    /** 余弦相似度，例如 0.94 */
    private Double score;

    /** 命中片段摘要（截断正文） */
    private String snippet;
}
