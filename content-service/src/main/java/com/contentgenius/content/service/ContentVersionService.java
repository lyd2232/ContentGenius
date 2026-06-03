package com.contentgenius.content.service;

import com.contentgenius.content.dto.CreateContentVersionRequest;
import com.contentgenius.content.dto.UpdateContentVersionRequest;
import com.contentgenius.content.entity.ContentVersion;

import java.util.List;

public interface ContentVersionService {

    ContentVersion create(Long projectId, CreateContentVersionRequest request);

    List<ContentVersion> listByProjectId(Long projectId);

    ContentVersion getById(Long id);

    ContentVersion update(Long id, UpdateContentVersionRequest request);


    void deleteVersion(Long id);
}
