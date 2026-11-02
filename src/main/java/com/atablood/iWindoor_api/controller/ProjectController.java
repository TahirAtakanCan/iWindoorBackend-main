package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.ProjectService;
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

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.createProject(project, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.getAllProjects(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.getProject(id, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser);
        return ResponseEntity.ok().build();
    }

    // MEO4: Maliyet Tablosu
    @GetMapping("/{id}/cost")
    public ResponseEntity<ProjectCostSummaryDTO> getCostSummary(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.getCostSummary(id, currentUser));
    }

    // MEO5: Teknik Özellikler
    @GetMapping("/{id}/specs")
    public ResponseEntity<ProjectSpecsDTO> getProjectSpecs(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projectService.getProjectSpecs(id, currentUser));
    }
}