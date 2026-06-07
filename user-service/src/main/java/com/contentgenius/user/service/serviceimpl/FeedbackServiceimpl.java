package com.contentgenius.user.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.common.sensitive.SensitiveWordChecker;
import com.contentgenius.user.entity.UserFeedback;
import com.contentgenius.user.mapper.UserFeedbackMapper;
import com.contentgenius.user.service.FeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FeedbackServiceimpl implements FeedbackService {

    private static final int MAX_PER_USER = 3;
    private static final int CONTENT_MAX_LEN = 500;

    private final UserFeedbackMapper userFeedbackMapper;

    public FeedbackServiceimpl(UserFeedbackMapper userFeedbackMapper) {
        this.userFeedbackMapper = userFeedbackMapper;
    }

    @Override
    public int maxPerUser() {
        return MAX_PER_USER;
    }

    @Override
    public int countByUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = userFeedbackMapper.selectCount(
                new QueryWrapper<UserFeedback>().eq("user_id", userId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public List<UserFeedback> listRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return userFeedbackMapper.selectList(new QueryWrapper<UserFeedback>()
                .orderByDesc("created_at")
                .last("LIMIT " + safeLimit));
    }

    @Override
    public UserFeedback submit(Long userId, String username, String content) {
        if (userId == null || !StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户信息无效");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写意见内容");
        }
        String trimmed = content.trim();
        SensitiveWordChecker.requireClean(trimmed);
        if (trimmed.length() > CONTENT_MAX_LEN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "意见最多 " + CONTENT_MAX_LEN + " 字");
        }
        if (countByUser(userId) >= MAX_PER_USER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每人最多提交 " + MAX_PER_USER + " 条意见");
        }

        UserFeedback row = new UserFeedback();
        row.setUserId(userId);
        row.setUsername(username.trim());
        row.setContent(trimmed);
        userFeedbackMapper.insert(row);
        return row;
    }
}
