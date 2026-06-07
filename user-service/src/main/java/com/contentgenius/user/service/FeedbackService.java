package com.contentgenius.user.service;

import com.contentgenius.user.entity.UserFeedback;

import java.util.List;

public interface FeedbackService {

    int maxPerUser();

    int countByUser(Long userId);

    List<UserFeedback> listRecent(int limit);

    UserFeedback submit(Long userId, String username, String content);
}
