package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto;
import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto.Request;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.ProjectExtraItem;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.ProjectExtraItemRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectExtraItemService {

    private final ProjectRepository projectRepository;
    private final ProjectExtraItemRepository extraItemRepository;
    private final SalesPricingService salesPricingService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public List<ProjectExtraItemDto> list(Long projectId, User user) {
        Project project = loadProject(projectId, user);
        return extraItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(project.getId()).stream()
                .map(ProjectExtraItemDto::of)
                .toList();
    }

    @Transactional
    public ProjectExtraItemDto create(Long projectId, Request req, User user) {
        Project project = loadProject(projectId, user);
        ProjectExtraItem item = new ProjectExtraItem();
        item.setProject(project);
        apply(item, req);
        ProjectExtraItem saved = extraItemRepository.save(item);
        salesPricingService.recalculateProjectSales(projectId);
        return ProjectExtraItemDto.of(saved);
    }

    @Transactional
    public ProjectExtraItemDto update(Long projectId, Long itemId, Request req, User user) {
        loadProject(projectId, user);
        ProjectExtraItem item = extraItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Ek kalem", itemId));
        apply(item, req);
        ProjectExtraItem saved = extraItemRepository.save(item);
        salesPricingService.recalculateProjectSales(projectId);
        return ProjectExtraItemDto.of(saved);
    }

    @Transactional
    public void delete(Long projectId, Long itemId, User user) {
        loadProject(projectId, user);
        ProjectExtraItem item = extraItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Ek kalem", itemId));
        extraItemRepository.delete(item);
        salesPricingService.recalculateProjectSales(projectId);
    }

    private Project loadProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        return project;
    }

    private static void apply(ProjectExtraItem item, Request req) {
        item.setName(req.name().trim());
        item.setQuantity(req.quantity().setScale(3, RoundingMode.HALF_UP));
        item.setUnit(req.unit().trim());
        item.setUnitPrice(req.unitPrice().setScale(2, RoundingMode.HALF_UP));
        item.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }
}
