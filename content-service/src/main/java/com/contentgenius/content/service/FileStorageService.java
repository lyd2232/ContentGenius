package com.contentgenius.content.service;

import com.contentgenius.content.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    FileUploadResponse upload(MultipartFile file);

    /** 列出当前用户在 MinIO 下的全部素材文件 */
    List<FileUploadResponse> listMine();

    FileUploadResponse load(String objectName);

    /** 删除当前用户目录下的对象（物理删除 MinIO 对象） */
    void delete(String objectName);
}
