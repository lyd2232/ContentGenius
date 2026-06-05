package com.contentgenius.content.service.impl;

import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.content.config.CurrentUser;
import com.contentgenius.content.config.MinioProperties;
import com.contentgenius.content.dto.FileUploadResponse;
import com.contentgenius.content.service.FileStorageService;
import com.contentgenius.content.util.MinioUtils;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储实现：上传到 MinIO，按 userId 目录隔离对象路径。
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceimpl implements FileStorageService {

    /** 预签名下载链接有效期：7 天 */
    private static final int PRESIGNED_URL_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    private final MinioUtils minioUtils;
    private final MinioProperties minioProperties;

    @Override
    public FileUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
        }
        String bucketName = minioProperties.resolvedBucketName();
        assertUnderFileQuota(bucketName);

        // 对象键：{userId}/{uuid}-{文件名}
        String objectName = buildObjectName(file.getOriginalFilename());
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";

        minioUtils.uploadFile(bucketName, file, objectName, contentType);

        // 返回短期可访问的预签名 URL
        String url = minioUtils.getPresignedObjectUrl(bucketName, objectName, PRESIGNED_URL_EXPIRE_SECONDS);
        FileUploadResponse response = new FileUploadResponse(objectName, url, contentType, file.getSize(), null);
        response.setDisplayName(extractDisplayName(objectName, file.getOriginalFilename()));
        return response;
    }

    @Override
    public List<FileUploadResponse> listMine() {
        String bucketName = minioProperties.resolvedBucketName();
        String prefix = CurrentUser.getUserId() + "/";
        List<Item> items = minioUtils.getAllObjectsByPrefix(bucketName, prefix, false);
        List<FileUploadResponse> result = new ArrayList<>();
        for (Item item : items) {
            if (item.isDir()) {
                continue;
            }
            String objectName = item.objectName();
            String url = minioUtils.getPresignedObjectUrl(bucketName, objectName, PRESIGNED_URL_EXPIRE_SECONDS);
            FileUploadResponse row = new FileUploadResponse(objectName, url, null, item.size(), null);
            row.setDisplayName(extractDisplayName(objectName, null));
            result.add(row);
        }
        result.sort(Comparator.comparing(FileUploadResponse::getObjectName).reversed());
        return result;
    }

    @Override
    public FileUploadResponse load(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径不能为空");
        }
        // 禁止跨用户访问他人目录下的 object
        assertOwnedObject(objectName);

        String bucketName = minioProperties.resolvedBucketName();
        if (!minioUtils.isObjectExist(bucketName, objectName)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        String url = minioUtils.getPresignedObjectUrl(bucketName, objectName, PRESIGNED_URL_EXPIRE_SECONDS);
        // 仅查链接时不回填 contentType、size
        FileUploadResponse response = new FileUploadResponse(objectName, url, null, null, null);
        response.setDisplayName(extractDisplayName(objectName, null));
        return response;
    }

    @Override
    public void delete(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径不能为空");
        }
        assertOwnedObject(objectName.trim());

        String bucketName = minioProperties.resolvedBucketName();
        if (!minioUtils.isObjectExist(bucketName, objectName)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        minioUtils.removeFile(bucketName, objectName);
    }

    /**
     * 生成 MinIO 对象名，去掉路径中的目录部分，防止 ../ 类路径穿越。
     */
    /** 对象键格式为 {userId}/{uuid}-{原始文件名} */
    private static String extractDisplayName(String objectName, String fallbackOriginal) {
        if (StringUtils.hasText(fallbackOriginal)) {
            String safe = fallbackOriginal.replace("\\", "/");
            int slash = safe.lastIndexOf('/');
            return slash >= 0 ? safe.substring(slash + 1) : safe;
        }
        if (!StringUtils.hasText(objectName)) {
            return "未命名文件";
        }
        String key = objectName.substring(objectName.lastIndexOf('/') + 1);
        int sep = key.indexOf('-');
        if (sep == 36 && key.length() > 37) {
            return key.substring(37);
        }
        if (sep > 0 && sep < key.length() - 1) {
            return key.substring(sep + 1);
        }
        return key;
    }

    private String buildObjectName(String originalFilename) {
        String safeName = StringUtils.hasText(originalFilename) ? originalFilename : "file";
        safeName = safeName.replace("\\", "/");
        int slash = safeName.lastIndexOf('/');
        if (slash >= 0) {
            safeName = safeName.substring(slash + 1);
        }
        return CurrentUser.getUserId() + "/" + UUID.randomUUID() + "-" + safeName;
    }

    /**
     * 仅允许访问当前用户目录下的对象（objectName 必须以 {userId}/ 开头）。
     */
    private void assertOwnedObject(String objectName) {
        String prefix = CurrentUser.getUserId() + "/";
        if (!objectName.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertUnderFileQuota(String bucketName) {
        int limit = minioProperties.resolvedMaxFilesPerUser();
        int count = countUserObjects(bucketName);
        if (count >= limit) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "最多保留 " + limit + " 个文件，请先删除旧文件后再上传");
        }
    }

    private int countUserObjects(String bucketName) {
        String prefix = CurrentUser.getUserId() + "/";
        List<Item> items = minioUtils.getAllObjectsByPrefix(bucketName, prefix, false);
        int count = 0;
        for (Item item : items) {
            if (!item.isDir()) {
                count++;
            }
        }
        return count;
    }
}
