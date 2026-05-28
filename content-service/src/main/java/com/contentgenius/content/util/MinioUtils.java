package com.contentgenius.content.util;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;

    public void ensureBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化 MinIO 桶失败: " + bucketName, e);
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new IllegalStateException("检查 MinIO 桶失败: " + bucketName, e);
        }
    }

    public String getBucketPolicy(String bucketName) {
        try {
            return minioClient.getBucketPolicy(
                    io.minio.GetBucketPolicyArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new IllegalStateException("获取桶策略失败: " + bucketName, e);
        }
    }

    public List<Bucket> getAllBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            throw new IllegalStateException("列出 MinIO 桶失败", e);
        }
    }

    public Optional<Bucket> getBucket(String bucketName) {
        return getAllBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    public void removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new IllegalStateException("删除 MinIO 桶失败: " + bucketName, e);
        }
    }

    public boolean isObjectExist(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return true;
        } catch (Exception e) {
            log.debug("对象不存在或不可访问: {}/{}", bucketName, objectName);
            return false;
        }
    }

    public boolean isFolderExist(String bucketName, String objectName) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(objectName).recursive(false).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir() && objectName.equals(item.objectName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("[Minio工具类] 判断文件夹是否存在异常", e);
        }
        return false;
    }

    public List<Item> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        try {
            List<Item> list = new ArrayList<>();
            Iterable<Result<Item>> objectsIterator = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
            for (Result<Item> o : objectsIterator) {
                list.add(o.get());
            }
            return list;
        } catch (Exception e) {
            throw new IllegalStateException("按前缀列出对象失败", e);
        }
    }

    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new IllegalStateException("读取对象失败: " + objectName, e);
        }
    }

    public InputStream getObject(String bucketName, String objectName, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .offset(offset)
                            .length(length)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("断点读取对象失败: " + objectName, e);
        }
    }

    public Iterable<Result<Item>> listObjects(String bucketName, String prefix, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(recursive).build());
    }

    public ObjectWriteResponse uploadFile(String bucketName, MultipartFile file, String objectName, String contentType) {
        try {
            ensureBucket(bucketName);
            InputStream inputStream = file.getInputStream();
            long size = file.getSize() >= 0 ? file.getSize() : inputStream.available();
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(inputStream, size, -1)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("上传文件失败: " + objectName, e);
        }
    }

    public ObjectWriteResponse uploadImage(String bucketName, String imageBase64, String imageName) {
        if (!StringUtils.hasText(imageBase64)) {
            return null;
        }
        InputStream in = base64ToInputStream(imageBase64);
        if (in == null) {
            return null;
        }
        String newName = System.currentTimeMillis() + "_" + imageName + ".jpg";
        LocalDate today = LocalDate.now();
        String objectName = today.getYear() + "/" + today.getMonthValue() + "/" + newName;
        return uploadFile(bucketName, objectName, in);
    }

    public static InputStream base64ToInputStream(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.error("Base64 解码失败", e);
            return null;
        }
    }

    public ObjectWriteResponse uploadFile(String bucketName, String objectName, String fileName) {
        try {
            ensureBucket(bucketName);
            return minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .filename(fileName)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("上传本地文件失败: " + fileName, e);
        }
    }

    public ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream) {
        try {
            ensureBucket(bucketName);
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("流式上传失败: " + objectName, e);
        }
    }

    public ObjectWriteResponse createDir(String bucketName, String objectName) {
        try {
            ensureBucket(bucketName);
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("创建目录失败: " + objectName, e);
        }
    }

    public String getFileStatusInfo(String bucketName, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build()).toString();
        } catch (Exception e) {
            throw new IllegalStateException("获取文件信息失败: " + objectName, e);
        }
    }

    public ObjectWriteResponse copyFile(String bucketName, String objectName,
                                        String destBucketName, String destObjectName) {
        try {
            return minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder().bucket(bucketName).object(objectName).build())
                            .bucket(destBucketName)
                            .object(destObjectName)
                            .build());
        } catch (Exception e) {
            throw new IllegalStateException("拷贝文件失败", e);
        }
    }

    public void removeFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new IllegalStateException("删除文件失败: " + objectName, e);
        }
    }

    public void removeFiles(String bucketName, List<String> keys) {
        for (String key : keys) {
            try {
                removeFile(bucketName, key);
            } catch (Exception e) {
                log.error("[Minio工具类] 批量删除文件异常: {}", key, e);
            }
        }
    }

    public String getPresignedObjectUrl(String bucketName, String objectName, Integer expiresSeconds) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiresSeconds, TimeUnit.SECONDS)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw new IllegalStateException("生成预签名 URL 失败", e);
        }
    }

    public String getPresignedObjectUrl(String bucketName, String objectName) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw new IllegalStateException("生成预签名 URL 失败", e);
        }
    }

    public String getUtf8ByURLDecoder(String str) throws UnsupportedEncodingException {
        String url = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
    }
}
