package com.contentgenius.agent.controller;

import com.contentgenius.agent.config.QdrantStorage;
import com.contentgenius.agent.dto.RagIngestRequest;
import com.contentgenius.agent.dto.RagSimilarityHit;
import com.contentgenius.agent.dto.RagSimilarityRequest;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/rag")
@RequiredArgsConstructor
public class RagController {

    private final QdrantStorage qdrantStorage;

    /**
     * 手动入库：将定稿正文写入 Qdrant（异步队列失败时可补调；需 JWT）。
     */
    @PostMapping("/ingest")
    public Result<Void> ingest(@Valid @RequestBody RagIngestRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String platform = StringUtils.hasText(request.getPlatform())
                ? request.getPlatform().trim()
                : "xiaohongshu";
        qdrantStorage.ingest(platform, userId, request.getVersionId(), request.getContent());
        return Result.ok();
    }

    /**
     * 返回相似度接口，使用于定稿模块
     */
    @PostMapping("/similarity")
    public Result<List<RagSimilarityHit>> similarity(@Valid @RequestBody RagSimilarityRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String platform = StringUtils.hasText(request.getPlatform())
                ? request.getPlatform().trim()
                : "xiaohongshu";
        List<RagSimilarityHit> hits = qdrantStorage.simsearch(
                platform, userId, request.getContent(), request.getVersionId());
        return Result.ok(hits);
    }

   //删除向量
    @DeleteMapping("/versions/{versionId}")
    public Result<Void> deleteVector(@PathVariable Long versionId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        qdrantStorage.deleteByVersionId(userId, versionId);
        return Result.ok();
    }
}
