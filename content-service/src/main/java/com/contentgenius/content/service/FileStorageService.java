package com.contentgenius.content.service;

import com.contentgenius.content.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse upload(MultipartFile file);

    FileUploadResponse load(String objectName);
}
