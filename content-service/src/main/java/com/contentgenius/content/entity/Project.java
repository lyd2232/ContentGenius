package com.contentgenius.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    /** 1进行中 2已归档 0已删除(软删) */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
