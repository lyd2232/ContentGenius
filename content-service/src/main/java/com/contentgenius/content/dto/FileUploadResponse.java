package com.contentgenius.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
//文件上传后响应
@Data
@NoArgsConstructor
@AllArgsConstructor
    public class FileUploadResponse {

    /** MinIO 对象路径（桶内 key） */
    private String objectName;

    /** 访问地址（直链或预签名 URL） */
    private String url;
//文件类型
    private String contentType;
//文件大小
    private Long size;
}
