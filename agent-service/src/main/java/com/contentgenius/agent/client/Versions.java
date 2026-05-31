package com.contentgenius.agent.client;

import com.contentgenius.agent.dto.ContentVersionDto;
import com.contentgenius.agent.dto.VersionRequest;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign：agent 写稿完成后，把正文存入 content-service 的 content_version。
 */
@FeignClient(name = "content-service", contextId = "contentVersionClient")
public interface Versions {

    @PostMapping("/api/content/projects/{projectId}/versions")
    Result<ContentVersionDto> create(
            @PathVariable("projectId") Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody VersionRequest request);
}
