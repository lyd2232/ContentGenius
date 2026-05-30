package com.contentgenius.agent.dto;

import lombok.Data;

/**
 * 与 content-service 返回的 template JSON 字段对应（Feign 反序列化用）。
 * <p>agent 不依赖 content 模块的 Template 实体，避免跨模块耦合。
 */
@Data
public class TemplateDto {

    /** 平台：xiaohongshu / wechat */
    private String platform;

    /**
     * 平台静态写法提示，对应库字段 prompt_hint。
     * 会写入 SystemMessage，约束语气、结构（与 6/17 RAG 历史稿互补）。
     */
    private String promptHint;
}
