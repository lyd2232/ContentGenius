package com.contentgenius.agent.client;


import com.contentgenius.agent.dto.UserLevelDto;
import com.contentgenius.agent.dto.VersionRequest;
import com.contentgenius.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "user-service",
        contextId = "getLevel")
public interface GetLevel {


    @GetMapping("/api/users/me")
    Result<UserLevelDto> me();

}
