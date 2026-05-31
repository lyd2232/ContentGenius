package com.contentgenius.agent.client;

import com.contentgenius.agent.dto.TemplateDto;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign 客户端：agent-service 远程调用 content-service 的模板只读接口。
 * <p>作用：按 platform 查询 template 表，拿到 prompt_hint 用于拼 System Prompt。
 */
// name 必须与 Nacos 注册的服务名一致，LoadBalancer 才能解析到 content-service 实例
@FeignClient(
        name = "content-service",
        contextId = "contentTemplateClient",
        path = "/api/content/templates")
public interface ContentTemplateClient {

    /**
     * 等价于 HTTP：GET /api/content/templates?platform=xiaohongshu
     *
     * @param platform 平台标识，与库表 template.platform 一致；可 null 表示查全部启用模板
     * @return 统一包装 Result，data 为模板列表（通常取第一条的 promptHint）
     */
    @GetMapping
    Result<List<TemplateDto>> listTemplates(@RequestParam(required = false) String platform);
}
