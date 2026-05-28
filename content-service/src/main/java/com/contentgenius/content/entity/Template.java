package com.contentgenius.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("template")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String platform;

    private String description;

    private String promptHint;

    /** 1启用 0停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
