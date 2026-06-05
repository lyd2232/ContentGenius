package com.contentgenius.content.controller;

import com.contentgenius.common.result.Result;
import com.contentgenius.content.dto.FileUploadResponse;
import com.contentgenius.content.service.FileStorageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/content/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
//上传文件
    @PostMapping("/upload")
    public Result<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.upload(file));
    }

    /** 列出当前用户已上传的素材（刷新页面后恢复列表） */
    @GetMapping
    public Result<List<FileUploadResponse>> list() {
        return Result.ok(fileStorageService.listMine());
    }

    @GetMapping("/url")
    public Result<FileUploadResponse> load(@RequestParam String objectName) {
        return Result.ok(fileStorageService.load(objectName));
    }

    /** 删除 MinIO 对象（仅能删本人 {userId}/ 下文件） */
    @DeleteMapping
    public Result<Void> delete(@RequestParam String objectName) {
        fileStorageService.delete(objectName);
        return Result.ok();
    }
}
