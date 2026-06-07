package com.contentgenius.user.controller;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.result.Result;
import com.contentgenius.user.entity.User;
import com.contentgenius.user.entity.UserFeedback;
import com.contentgenius.user.service.FeedbackService;
import com.contentgenius.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Object>> list() {
        User user = currentUser();
        List<UserFeedback> rows = feedbackService.listRecent(50);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", rows.stream().map(this::toItem).collect(Collectors.toList()));
        body.put("myCount", feedbackService.countByUser(user.getId()));
        body.put("maxPerUser", feedbackService.maxPerUser());
        return Result.ok(body);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:read')")
    public Result<Map<String, Object>> submit(@RequestBody SubmitFeedbackRequest request) {
        User user = currentUser();
        UserFeedback saved = feedbackService.submit(user.getId(), user.getUsername(), request.content());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", saved.getId());
        body.put("myCount", feedbackService.countByUser(user.getId()));
        body.put("maxPerUser", feedbackService.maxPerUser());
        return Result.ok(body);
    }

    private Map<String, Object> toItem(UserFeedback row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("username", row.getUsername());
        item.put("content", row.getContent());
        item.put("createdAt", row.getCreatedAt());
        return item;
    }

    private User currentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public record SubmitFeedbackRequest(String content) {
    }
}
