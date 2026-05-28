package com.contentgenius.content.controller;

import com.contentgenius.common.result.Result;
import com.contentgenius.content.dto.CreateContentVersionRequest;
import com.contentgenius.content.dto.UpdateContentVersionRequest;
import com.contentgenius.content.entity.ContentVersion;
import com.contentgenius.content.service.ContentVersionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContentVersionController {

    private final ContentVersionService contentVersionService;

    public ContentVersionController(ContentVersionService contentVersionService) {
        this.contentVersionService = contentVersionService;
    }
//在某个项目下新建一版
    @PostMapping("/api/content/projects/{projectId}/versions")
    public Result<ContentVersion> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateContentVersionRequest request) {
        return Result.ok(contentVersionService.create(projectId, request));
    }
//列出某个项目下的所有版本
    @GetMapping("/api/content/projects/{projectId}/versions")
    public Result<List<ContentVersion>> list(@PathVariable Long projectId) {
        return Result.ok(contentVersionService.listByProjectId(projectId));
    }
//查询单个版本详情
    @GetMapping("/api/content/versions/{id}")
    public Result<ContentVersion> get(@PathVariable Long id) {
        return Result.ok(contentVersionService.getById(id));
    }
//修改版本
    @PutMapping("/api/content/versions/{id}")
    public Result<ContentVersion> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentVersionRequest request) {
        return Result.ok(contentVersionService.update(id, request));
    }
}
