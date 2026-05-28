package com.contentgenius.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_version")
public class ContentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Integer versionNo;

    private String title;

    private String content;

    private String platform;

    /** agent / manual / import */
    private String source;

    /** 0草稿 1已定稿 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
