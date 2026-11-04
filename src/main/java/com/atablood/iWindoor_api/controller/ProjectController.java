package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.dto.project.ProjectRequest;
import com.atablood.iWindoor_api.dto.window.WindowCreateRequest;
import com.atablood.iWindoor_api.dto.window.WindowDto;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.ProjectService;
import com.atablood.iWindoor_api.service.WindowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final WindowService windowService;

    @PostMapping
    public Project createProject(@Valid @RequestBody ProjectRequest request, @AuthenticationPrincipal User currentUser) {
        return projectService.createProject(request, currentUser);
    }

    @GetMapping
    public List<Project> getAllProjects(@AuthenticationPrincipal User currentUser) {
        return projectService.getAllProjects(currentUser);
    }

    @GetMapping("/{id}")
    public Project getProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return projectService.getProject(id, currentUser);
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        return projectService.updateProject(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser);
        return ResponseEntity.ok().build();
    }

    /** Pencere ekle. Eski uygulama {name,width,height}; yeni istemci ek olarak seriesId ve design gönderebilir. */
    @PostMapping("/{id}/windows")
    public WindowDto addWindow(@PathVariable Long id, @Valid @RequestBody WindowCreateRequest request,
                               @AuthenticationPrincipal User currentUser) {
        return windowService.create(id, request, currentUser);
    }

    @PostMapping("/{id}/calculate-price")
    public ResponseEntity<Void> calculatePrice(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        projectService.calculatePrice(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/sync-prices")
    public ResponseEntity<Void> syncPrices(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        projectService.syncPrices(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/{id}/cost", "/{id}/cost-summary"})
    public ProjectCostSummaryDTO getCostSummary(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return projectService.getCostSummary(id, currentUser);
    }

    @GetMapping("/{id}/specs")
    public ProjectSpecsDTO getProjectSpecs(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return projectService.getProjectSpecs(id, currentUser);
    }
}
