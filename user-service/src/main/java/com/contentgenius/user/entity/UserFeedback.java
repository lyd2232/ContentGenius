package com.contentgenius.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_feedback")
public class UserFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String content;

    private LocalDateTime createdAt;
}
