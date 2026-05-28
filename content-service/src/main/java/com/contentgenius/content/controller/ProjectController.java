package com.contentgenius.content.controller;

import com.contentgenius.common.result.Result;
import com.contentgenius.content.dto.CreateProjectRequest;
import com.contentgenius.content.dto.UpdateProjectRequest;
import com.contentgenius.content.entity.Project;
import com.contentgenius.content.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }
//创建项目在当前用户下
    @PostMapping
    public Result<Project> create(@Valid @RequestBody CreateProjectRequest request) {
        return Result.ok(projectService.create(request));
    }
//获取当前用户下的项目列表
    @GetMapping
    public Result<List<Project>> list() {
        return Result.ok(projectService.listMine());
    }
//获取项目详情
    @GetMapping("/{id}")
    public Result<Project> get(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }
//更新项目
    @PutMapping("/{id}")
    public Result<Project> update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
        return Result.ok(projectService.update(id, request));
    }
//删除项目
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }
}
