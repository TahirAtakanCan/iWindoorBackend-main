package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User; // Ekle
import java.util.List;

public interface ProjectService {
    // Artık metodlar 'User' parametresi alıyor
    Project createProject(Project project, User currentUser);

    List<Project> getAllProjects(User currentUser);

    Project getProject(Long id, User currentUser);

    void deleteProject(Long id, User currentUser);

    ProjectCostSummaryDTO getCostSummary(Long projectId, User currentUser);

    ProjectSpecsDTO getProjectSpecs(Long projectId, User currentUser);
}