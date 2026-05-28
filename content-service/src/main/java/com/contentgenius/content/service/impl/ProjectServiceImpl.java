package com.contentgenius.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contentgenius.common.exception.BusinessException;
import com.contentgenius.common.exception.ErrorCode;
import com.contentgenius.content.config.CurrentUser;
import com.contentgenius.content.dto.CreateProjectRequest;
import com.contentgenius.content.dto.UpdateProjectRequest;
import com.contentgenius.content.entity.Project;
import com.contentgenius.content.mapper.ProjectMapper;
import com.contentgenius.content.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 项目业务实现：CRUD + 按当前登录用户隔离数据。
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    /** 软删标记：列表与详情均不可见 */
    private static final int STATUS_DELETED = 0;
    /** 进行中，新建项目默认状态 */
    private static final int STATUS_ACTIVE = 1;
    /** 已归档 */
    private static final int STATUS_ARCHIVED = 2;

    /** MyBatis-Plus 项目表访问 */
    private final ProjectMapper projectMapper;

    /** 构造注入 Mapper */
    public ProjectServiceImpl(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public Project create(CreateProjectRequest request) {
        // 组装实体，userId 来自 JWT，不信任请求体
        Project project = new Project();
        project.setUserId(CurrentUser.getUserId());
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setStatus(STATUS_ACTIVE);
        // 插入后 project.id 由数据库回填
        projectMapper.insert(project);
        return project;
    }

    @Override
    public List<Project> listMine() {
        // 只查当前用户的、未软删的项目，按更新时间倒序
        Long userId = CurrentUser.getUserId();
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .ne(Project::getStatus, STATUS_DELETED)
                .orderByDesc(Project::getUpdatedAt));
    }

    @Override
    public Project getById(Long id) {
        // 必须存在、未删除且属于当前用户
        return requireOwnedActiveProject(id);
    }

    @Override
    public Project update(Long id, UpdateProjectRequest request) {
        Project project = requireOwnedActiveProject(id);
        // 有标题才覆盖，避免把标题更新成空
        if (StringUtils.hasText(request.getTitle())) {
            project.setTitle(request.getTitle());
        }
        // description 允许传空字符串清空
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            // 仅允许在「进行中」与「已归档」之间切换
            if (request.getStatus() != STATUS_ACTIVE && request.getStatus() != STATUS_ARCHIVED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "状态仅支持 1(进行中) 或 2(已归档)");
            }
            project.setStatus(request.getStatus());
        }
        projectMapper.updateById(project);
        return project;
    }

    @Override
    public void delete(Long id) {
        // 软删：改 status，不物理删除行
        Project project = requireOwnedActiveProject(id);
        project.setStatus(STATUS_DELETED);
        projectMapper.updateById(project);
    }

    /**
     * 查询有效项目并校验归属当前用户。
     */
    private Project requireOwnedActiveProject(Long id) {
        Project project = requireActiveProject(id);
        assertOwner(project);
        return project;
    }

    /**
     * 按主键查项目：不存在或已软删则 404。
     */
    private Project requireActiveProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getStatus() == null || project.getStatus() == STATUS_DELETED) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    /**
     * 项目 userId 必须与 Token 中的 userId 一致。
     */
    private void assertOwner(Project project) {
        if (!Objects.equals(project.getUserId(), CurrentUser.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
