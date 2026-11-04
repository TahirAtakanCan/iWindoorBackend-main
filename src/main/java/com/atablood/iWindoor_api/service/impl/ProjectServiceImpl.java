package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.dto.project.ProjectRequest;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.entity.WindowUnit;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.atablood.iWindoor_api.service.PricingService;
import com.atablood.iWindoor_api.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final PricingService pricingService;
    private final AccessGuard guard;

    @Override
    @Transactional
    public Project createProject(ProjectRequest request, User currentUser) {
        guard.companyId(currentUser);
        Project project = new Project();
        applyRequest(project, request);
        project.setCreatedBy(currentUser);
        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public Project updateProject(Long id, ProjectRequest request, User currentUser) {
        Project project = getProject(id, currentUser);
        applyRequest(project, request);
        return projectRepository.save(project);
    }

    private static void applyRequest(Project project, ProjectRequest request) {
        project.setName(request.name());
        project.setDescription(request.description());
        project.setCustomerName(request.customerName());
        project.setCustomerPhone(request.customerPhone());
        project.setCustomerEmail(request.customerEmail());
        project.setCustomerAddress(request.customerAddress());
    }

    @Override
    public List<Project> getAllProjects(User currentUser) {
        return projectRepository.findAllByCreatedBy_Company_Id(guard.companyId(currentUser));
    }

    @Override
    public Project getProject(Long id, User currentUser) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Proje", id));
        guard.checkProject(project, currentUser);
        return project;
    }

    @Override
    @Transactional
    public void deleteProject(Long id, User currentUser) {
        projectRepository.delete(getProject(id, currentUser));
    }

    @Override
    @Transactional
    public void calculatePrice(Long id, User currentUser) {
        getProject(id, currentUser);
        pricingService.calculateProjectPrice(id);
    }

    @Override
    @Transactional
    public void syncPrices(Long id, User currentUser) {
        getProject(id, currentUser);
        pricingService.syncPrices(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectCostSummaryDTO getCostSummary(Long projectId, User currentUser) {
        return pricingService.calculateCostSummary(getProject(projectId, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectSpecsDTO getProjectSpecs(Long projectId, User currentUser) {
        Project project = getProject(projectId, currentUser);
        ProjectSpecsDTO specs = new ProjectSpecsDTO();
        specs.setProjectName(project.getName());
        specs.setTotalWindowCount(project.getWindowUnits().size());

        double totalArea = 0, profileM = 0;
        Set<String> profileNames = new LinkedHashSet<>();
        Set<String> glassNames = new LinkedHashSet<>();

        for (WindowUnit unit : project.getWindowUnits()) {
            int q = unit.getQuantity() == null || unit.getQuantity() < 1 ? 1 : unit.getQuantity();
            if (unit.getWidth() != null && unit.getHeight() != null) {
                totalArea += unit.getWidth() * unit.getHeight() / 1_000_000.0 * q;
            }
            if (unit.getBomSnapshot() != null) {
                for (Map<String, Object> l : unit.getBomSnapshot()) {
                    Object group = l.get("group");
                    if ("PROFILE".equals(group) && l.get("length") instanceof Number len && l.get("qty") instanceof Number n) {
                        profileM += len.doubleValue() / 1000.0 * n.intValue() * q;
                        if (l.get("role") instanceof String r) profileNames.add(r);
                    }
                    if (("GLASS".equals(group) || "PANEL".equals(group)) && l.get("glassCode") instanceof String g) {
                        glassNames.add(g);
                    }
                }
            } else if (unit.getRootNode() != null) {
                collectSpecsRecursive(unit.getRootNode(), profileNames, glassNames);
            }
        }
        specs.setTotalAreaM2(Math.round(totalArea * 100.0) / 100.0);
        specs.setTotalProfileLengthM(Math.round(profileM * 100.0) / 100.0);
        specs.setUsedProfileTypes(new ArrayList<>(profileNames));
        specs.setUsedGlassTypes(new ArrayList<>(glassNames));
        return specs;
    }

    private void collectSpecsRecursive(WindowNode node, Set<String> profiles, Set<String> glasses) {
        if (node.getProfile() != null) profiles.add(node.getProfile().getName());
        if (node.getGlass() != null) glasses.add(node.getGlass().getName());
        if (node.getChildren() != null) for (WindowNode child : node.getChildren()) collectSpecsRecursive(child, profiles, glasses);
    }
}
