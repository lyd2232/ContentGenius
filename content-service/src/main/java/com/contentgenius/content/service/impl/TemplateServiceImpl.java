package com.contentgenius.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contentgenius.content.entity.Template;
import com.contentgenius.content.mapper.TemplateMapper;
import com.contentgenius.content.service.TemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 写作模板只读查询：供前端/Agent 选择平台风格
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    /** 库表 status=1 表示启用 */
    private static final int STATUS_ENABLED = 1;

    private final TemplateMapper templateMapper;

    public TemplateServiceImpl(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public List<Template> listEnabled(String platform) {
        // 基础条件：仅启用模板，按平台、id 排序
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(Template::getStatus, STATUS_ENABLED)
                .orderByAsc(Template::getPlatform)
                .orderByAsc(Template::getId);
        // 可选按平台过滤，如 xiaohongshu / wechat
        if (StringUtils.hasText(platform)) {
            wrapper.eq(Template::getPlatform, platform.trim());
        }
        return templateMapper.selectList(wrapper);
    }
}
