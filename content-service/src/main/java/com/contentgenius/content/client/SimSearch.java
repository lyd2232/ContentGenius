package com.contentgenius.content.client;

import com.contentgenius.common.result.Result;
import com.contentgenius.content.dto.RagSimilarityHit;
import com.contentgenius.content.dto.RagSimilarityRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "agent-service", contextId = "simSearchClient", path = "/api/agent/rag")
public interface SimSearch {

    @PostMapping("/similarity")
    Result<List<RagSimilarityHit>> similarity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RagSimilarityRequest request);

    @DeleteMapping("/versions/{versionId}")
    Result<Void> deleteVector(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("versionId") Long versionId);
}
