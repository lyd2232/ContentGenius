package com.contentgenius.content.service;

import com.contentgenius.content.entity.Template;

import java.util.List;

public interface TemplateService {

    /**
     * 启用中的模板列表；platform 为空则返回全部平台。
     */
    List<Template> listEnabled(String platform);
}
