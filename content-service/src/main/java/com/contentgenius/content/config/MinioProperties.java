package com.contentgenius.content.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Nacos 中 minio.* 配置绑定 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    /** 对应 minio.bucket-name */
    private String bucketName;
    /** 对应 minio.bucket */
    private String bucket;

    /** 每用户最多保留的上传文件数（MinIO 对象），默认 3 */
    private Integer maxFilesPerUser = 3;

    /** 解析桶名：bucket-name 与 bucket 二选一 */
    public String resolvedBucketName() {
        String value = StringUtils.hasText(bucketName) ? bucketName : bucket;
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Nacos 缺少 minio.bucket 或 minio.bucket-name");
        }
        return value.trim();
    }

    public int resolvedMaxFilesPerUser() {
        return maxFilesPerUser != null && maxFilesPerUser > 0 ? maxFilesPerUser : 3;
    }
}
