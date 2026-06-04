package com.contentgenius.content.service;

import com.contentgenius.content.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse upload(MultipartFile file);

    FileUploadResponse load(String objectName);

    /** 删除当前用户目录下的对象（物理删除 MinIO 对象） */
    void delete(String objectName);
}
