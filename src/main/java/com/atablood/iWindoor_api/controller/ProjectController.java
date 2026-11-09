package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.ProjectCostSummaryDTO;
import com.atablood.iWindoor_api.dto.ProjectSpecsDTO;
import com.atablood.iWindoor_api.dto.project.*;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto;
import com.atablood.iWindoor_api.dto.window.WindowCreateRequest;
import com.atablood.iWindoor_api.dto.window.WindowDto;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.ProjectStatus;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final WindowService windowService;
    private final ProjectQuoteService projectQuoteService;
    private final ProjectFileService projectFileService;
    private final ProjectExtraItemService extraItemService;
    private final ProjectStatusService projectStatusService;
    private final PaymentService paymentService;
    private final QuoteVersionService quoteVersionService;
    private final SurveyService surveyService;

    @PostMapping
    public Project createProject(@Valid @RequestBody ProjectRequest request, @AuthenticationPrincipal User currentUser) {
        return projectService.createProject(request, currentUser);
    }

    @PostMapping("/import")
    public Project importProject(@RequestBody ProjectFileDto file, @AuthenticationPrincipal User currentUser) {
        return projectFileService.importFile(file, currentUser);
    }

    @GetMapping
    public List<Project> getAllProjects(@RequestParam(required = false) ProjectStatus status,
                                        @RequestParam(required = false) Long customerId,
                                        @AuthenticationPrincipal User currentUser) {
        return projectService.getAllProjects(currentUser, status, customerId);
    }

    @GetMapping("/{id}")
    public Project getProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return projectService.getProject(id, currentUser);
    }

    @GetMapping("/{id}/quote")
    public ProjectQuoteDto quote(@PathVariable Long id,
                                 @RequestParam(required = false) BigDecimal discountPct,
                                 @AuthenticationPrincipal User currentUser) {
        return projectQuoteService.quote(id, discountPct, currentUser);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<ProjectFileDto> export(@PathVariable Long id,
                                                 @AuthenticationPrincipal User currentUser) {
        return projectFileService.export(id, currentUser);
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

    @GetMapping("/{id}/extras")
    public List<ProjectExtraItemDto> listExtras(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return extraItemService.list(id, currentUser);
    }

    @PostMapping("/{id}/extras")
    public ProjectExtraItemDto createExtra(@PathVariable Long id, @Valid @RequestBody ProjectExtraItemDto.Request request,
                                           @AuthenticationPrincipal User currentUser) {
        return extraItemService.create(id, request, currentUser);
    }

    @PutMapping("/{id}/extras/{itemId}")
    public ProjectExtraItemDto updateExtra(@PathVariable Long id, @PathVariable Long itemId,
                                           @Valid @RequestBody ProjectExtraItemDto.Request request,
                                           @AuthenticationPrincipal User currentUser) {
        return extraItemService.update(id, itemId, request, currentUser);
    }

    @DeleteMapping("/{id}/extras/{itemId}")
    public ResponseEntity<Void> deleteExtra(@PathVariable Long id, @PathVariable Long itemId,
                                            @AuthenticationPrincipal User currentUser) {
        extraItemService.delete(id, itemId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    public Project changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request,
                                @AuthenticationPrincipal User currentUser) {
        return projectStatusService.changeStatus(id, request, currentUser);
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryDto> history(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return projectStatusService.history(id, currentUser);
    }

    @PutMapping("/{id}/delivery-date")
    public Project setDeliveryDate(@PathVariable Long id, @RequestBody DeliveryDateRequest request,
                                   @AuthenticationPrincipal User currentUser) {
        return projectStatusService.setDeliveryDate(id, request == null ? null : request.deliveryDate(), currentUser);
    }

    @GetMapping("/{id}/payments")
    public PaymentDto.ListResponse listPayments(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return paymentService.list(id, currentUser);
    }

    @PostMapping("/{id}/payments")
    public PaymentDto createPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request,
                                    @AuthenticationPrincipal User currentUser) {
        return paymentService.create(id, request, currentUser);
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id, @PathVariable Long paymentId,
                                              @AuthenticationPrincipal User currentUser) {
        paymentService.delete(id, paymentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    public List<com.atablood.iWindoor_api.dto.quote.QuoteVersionDto> listVersions(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return quoteVersionService.list(id, currentUser);
    }

    @PostMapping("/{id}/versions")
    public com.atablood.iWindoor_api.dto.quote.QuoteVersionDto createVersion(
            @PathVariable Long id,
            @RequestBody(required = false) com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return quoteVersionService.create(id, request, currentUser);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.DetailDto getVersion(
            @PathVariable Long id, @PathVariable Long versionId, @AuthenticationPrincipal User currentUser) {
        return quoteVersionService.get(id, versionId, currentUser);
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.RestoreResponse restoreVersion(
            @PathVariable Long id, @PathVariable Long versionId, @AuthenticationPrincipal User currentUser) {
        return quoteVersionService.restore(id, versionId, currentUser);
    }

    @DeleteMapping("/{id}/versions/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long id, @PathVariable Long versionId,
                                              @AuthenticationPrincipal User currentUser) {
        quoteVersionService.delete(id, versionId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/survey")
    public List<com.atablood.iWindoor_api.dto.survey.SurveyItemDto> listSurvey(
            @PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return surveyService.list(id, currentUser);
    }

    @PostMapping("/{id}/survey")
    public ResponseEntity<com.atablood.iWindoor_api.dto.survey.SurveyItemDto> createSurvey(
            @PathVariable Long id,
            @Valid @RequestBody com.atablood.iWindoor_api.dto.survey.SurveyItemDto.Request request,
            @AuthenticationPrincipal User currentUser) {
        var result = surveyService.create(id, request, currentUser);
        return ResponseEntity.status(result.created()
                        ? org.springframework.http.HttpStatus.CREATED
                        : org.springframework.http.HttpStatus.OK)
                .body(result.body());
    }

    @PutMapping("/{id}/survey/{itemId}")
    public com.atablood.iWindoor_api.dto.survey.SurveyItemDto updateSurvey(
            @PathVariable Long id, @PathVariable Long itemId,
            @Valid @RequestBody com.atablood.iWindoor_api.dto.survey.SurveyItemDto.Request request,
            @AuthenticationPrincipal User currentUser) {
        return surveyService.update(id, itemId, request, currentUser);
    }

    @DeleteMapping("/{id}/survey/{itemId}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id, @PathVariable Long itemId,
                                             @AuthenticationPrincipal User currentUser) {
        surveyService.delete(id, itemId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/survey/{itemId}/link")
    public com.atablood.iWindoor_api.dto.survey.SurveyItemDto linkSurvey(
            @PathVariable Long id, @PathVariable Long itemId,
            @RequestBody com.atablood.iWindoor_api.dto.survey.SurveyItemDto.LinkRequest request,
            @AuthenticationPrincipal User currentUser) {
        return surveyService.link(id, itemId, request, currentUser);
    }

    @PostMapping(value = "/{id}/survey/{itemId}/photos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, Long>> uploadSurveyPhoto(
            @PathVariable Long id, @PathVariable Long itemId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "clientId", required = false) String clientId,
            @AuthenticationPrincipal User currentUser) {
        var result = surveyService.uploadPhoto(id, itemId, file, clientId, currentUser);
        return ResponseEntity.status(result.created()
                        ? org.springframework.http.HttpStatus.CREATED
                        : org.springframework.http.HttpStatus.OK)
                .body(java.util.Map.of("id", result.id()));
    }
}
