package com.contentgenius.content.service;

import com.contentgenius.content.dto.CreateProjectRequest;
import com.contentgenius.content.dto.UpdateProjectRequest;
import com.contentgenius.content.entity.Project;

import java.util.List;

public interface ProjectService {

    Project create(CreateProjectRequest request);

    List<Project> listMine();

    Project getById(Long id);

    Project update(Long id, UpdateProjectRequest request);

    void delete(Long id);
}
