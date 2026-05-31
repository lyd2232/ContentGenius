package com.contentgenius.agent.controller;

import com.contentgenius.agent.dto.AgentChatRequest;
import com.contentgenius.agent.dto.AgentChatResponse;
import com.contentgenius.agent.service.AgentChatService;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;

//回答入口
    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        AgentChatResponse data = agentChatService.chat(request);
        return Result.ok(data);
    }
    //流式
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentChatResponse> chatStream(@Valid @RequestBody AgentChatRequest request) {
        Flux<AgentChatResponse> data = agentChatService.chatStream(request);
        return data;
    }
}
