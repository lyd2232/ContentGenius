package com.contentgenius.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.content.dto.CreateContentVersionRequest;
import com.contentgenius.content.dto.UpdateContentVersionRequest;
import com.contentgenius.content.entity.ContentVersion;
import com.contentgenius.content.mapper.ContentVersionMapper;
import com.contentgenius.content.service.ContentVersionService;
import com.contentgenius.content.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 内容版本业务实现：同一 project 下多版本，版本号自增。
 */
@Service
public class ContentVersionServiceImpl implements ContentVersionService {

    /** 草稿 */
    private static final int STATUS_DRAFT = 0;
    /** 已定稿 */
    private static final int STATUS_FINALIZED = 1;

    /** 未传 source 时的默认值 */
    private static final String SOURCE_MANUAL = "manual";
    /** 允许的内容来源枚举 */
    private static final Set<String> ALLOWED_SOURCES = Set.of("manual", "agent", "import");

    /** 用于校验 project 归属（getById 内含 owner 检查） */
    private final ProjectService projectService;
    private final ContentVersionMapper contentVersionMapper;

    public ContentVersionServiceImpl(ProjectService projectService, ContentVersionMapper contentVersionMapper) {
        this.projectService = projectService;
        this.contentVersionMapper = contentVersionMapper;
    }

    @Override
    public ContentVersion create(Long projectId, CreateContentVersionRequest request) {
        // 项目不存在或非本人会在这里抛异常
        projectService.getById(projectId);

        ContentVersion version = new ContentVersion();
        version.setProjectId(projectId);
        version.setVersionNo(nextVersionNo(projectId));
        version.setTitle(request.getTitle());
        version.setContent(request.getContent());
        version.setPlatform(request.getPlatform());
        version.setSource(resolveSource(request.getSource()));
        version.setStatus(STATUS_DRAFT);
        contentVersionMapper.insert(version);
        return version;
    }

    @Override
    public List<ContentVersion> listByProjectId(Long projectId) {
        // 先校验项目权限，再查该项目下全部版本
        projectService.getById(projectId);
        return contentVersionMapper.selectList(new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getProjectId, projectId)
                .orderByDesc(ContentVersion::getVersionNo));
    }

    @Override
    public ContentVersion getById(Long id) {
        return requireVersion(id);
    }

    @Override
    public ContentVersion update(Long id, UpdateContentVersionRequest request) {
        ContentVersion version = requireVersion(id);
        if (StringUtils.hasText(request.getTitle())) {
            version.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            version.setContent(request.getContent());
        }
        if (request.getPlatform() != null) {
            version.setPlatform(request.getPlatform());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != STATUS_DRAFT && request.getStatus() != STATUS_FINALIZED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 0(草稿) 或 1(已定稿)");
            }
            version.setStatus(request.getStatus());
        }
        contentVersionMapper.updateById(version);
        return version;
    }

    /**
     * 取该项目当前最大 versionNo，新记录 = max + 1；无历史版本则从 1 开始。
     */
    private int nextVersionNo(Long projectId) {
        ContentVersion latest = contentVersionMapper.selectOne(new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getProjectId, projectId)
                .orderByDesc(ContentVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    /**
     * 规范化来源字段：空则 manual，否则转小写并校验白名单。
     */
    private String resolveSource(String source) {
        if (!StringUtils.hasText(source)) {
            return SOURCE_MANUAL;
        }
        String normalized = source.trim().toLowerCase();
        if (!ALLOWED_SOURCES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "来源仅支持 manual、agent、import");
        }
        return normalized;
    }

    /**
     * 版本必须存在，且其所属 project 对当前用户可访问。
     */
    private ContentVersion requireVersion(Long id) {
        ContentVersion version = contentVersionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ErrorCode.VERSION_NOT_FOUND);
        }
        // 通过 project 归属间接校验版本权限
        projectService.getById(version.getProjectId());
        return version;
    }
}
