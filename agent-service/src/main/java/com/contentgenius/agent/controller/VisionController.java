package com.contentgenius.agent.controller;

import com.contentgenius.agent.dto.VisionAnalyzeRequest;
import com.contentgenius.agent.dto.VisionAnalyzeResponse;
import com.contentgenius.agent.service.VisionService;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;


    @PostMapping("/analyze")
    public Result<VisionAnalyzeResponse> analyze(@Valid @RequestBody VisionAnalyzeRequest request) {
        return Result.ok(visionService.analyze(request));
    }
}
