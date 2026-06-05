package com.contentgenius.agent.client;

import com.contentgenius.agent.dto.FileUploadResponse;
import com.contentgenius.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "content-service", contextId = "contentFileClient", path = "/api/content/files")
public interface ContentFileClient {
    @GetMapping("/url")
    Result<FileUploadResponse> load(@RequestParam("objectName") String objectName);
}