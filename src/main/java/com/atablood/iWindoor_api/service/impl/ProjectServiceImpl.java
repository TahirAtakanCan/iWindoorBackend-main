package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.dto.project.ProjectRequest;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.atablood.iWindoor_api.service.CustomerService;
import com.atablood.iWindoor_api.service.PricingService;
import com.atablood.iWindoor_api.service.ProjectFinanceService;
import com.atablood.iWindoor_api.service.ProjectService;
import com.atablood.iWindoor_api.service.SalesPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final PricingService pricingService;
    private final SalesPricingService salesPricingService;
    private final ProjectFinanceService financeService;
    private final CustomerService customerService;
    private final AccessGuard guard;

    @Override
    @Transactional
    public Project createProject(ProjectRequest request, User currentUser) {
        Long companyId = guard.companyId(currentUser);
        Project project = new Project();
        applyRequest(project, request, companyId);
        project.setCreatedBy(currentUser);
        project.setStatus(ProjectStatus.QUOTE);
        Project saved = projectRepository.save(project);
        financeService.enrich(saved, currentUser);
        return saved;
    }

    @Override
    @Transactional
    public Project updateProject(Long id, ProjectRequest request, User currentUser) {
        Project project = getProject(id, currentUser);
        applyRequest(project, request, guard.companyId(currentUser));
        Project saved = projectRepository.save(project);
        financeService.enrich(saved, currentUser);
        return saved;
    }

    private void applyRequest(Project project, ProjectRequest request, Long companyId) {
        project.setName(request.name());
        project.setDescription(request.description());
        if (request.customerId() != null) {
            Customer customer = customerService.requireForCompany(request.customerId(), companyId);
            project.setCustomer(customer);
            project.setCustomerName(customer.getName());
            project.setCustomerPhone(customer.getPhone());
            project.setCustomerEmail(customer.getEmail());
            project.setCustomerAddress(customer.getAddress());
        } else if (request.customerName() != null && !request.customerName().isBlank()) {
            project.setCustomerName(request.customerName());
            project.setCustomerPhone(request.customerPhone());
            project.setCustomerEmail(request.customerEmail());
            project.setCustomerAddress(request.customerAddress());
            Customer customer = customerService.findOrCreate(companyId, request.customerName(),
                    request.customerPhone(), request.customerEmail(), request.customerAddress());
            project.setCustomer(customer);
        } else {
            project.setCustomerName(request.customerName());
            project.setCustomerPhone(request.customerPhone());
            project.setCustomerEmail(request.customerEmail());
            project.setCustomerAddress(request.customerAddress());
        }
    }

    @Override
    @Transactional
    public List<Project> getAllProjects(User currentUser, ProjectStatus status, Long customerId) {
        Long companyId = guard.companyId(currentUser);
        List<Project> projects;
        if (status != null && customerId != null) {
            projects = projectRepository.findAllByCreatedBy_Company_IdAndStatusAndCustomer_Id(companyId, status, customerId);
        } else if (status != null) {
            projects = projectRepository.findAllByCreatedBy_Company_IdAndStatus(companyId, status);
        } else if (customerId != null) {
            projects = projectRepository.findAllByCreatedBy_Company_IdAndCustomer_Id(companyId, customerId);
        } else {
            projects = projectRepository.findAllByCreatedBy_Company_Id(companyId);
        }
        for (Project p : projects) {
            if (p.getSaleTotal() == null) salesPricingService.ensureSaleTotal(p);
            financeService.enrich(p, currentUser);
        }
        return projects;
    }

    @Override
    @Transactional
    public Project getProject(Long id, User currentUser) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Proje", id));
        guard.checkProject(project, currentUser);
        if (project.getSaleTotal() == null) salesPricingService.ensureSaleTotal(project);
        project = projectRepository.findById(id).orElse(project);
        financeService.enrich(project, currentUser);
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
