package com.contentgenius.content.controller;

import com.contentgenius.common.result.Result;
import com.contentgenius.content.entity.Template;
import com.contentgenius.content.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }
//获取模板列表
    @GetMapping
    public Result<List<Template>> list(@RequestParam(required = false) String platform) {
        return Result.ok(templateService.listEnabled(platform));
    }
}
