package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.template.DesignTemplateCreateRequest;
import com.atablood.iWindoor_api.dto.template.DesignTemplateDto;
import com.atablood.iWindoor_api.dto.template.DesignTemplateUpdateRequest;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.DesignTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class DesignTemplateController {

    private final DesignTemplateService designTemplateService;

    @GetMapping
    public List<DesignTemplateDto> list(@RequestParam(required = false) String category,
                                        @AuthenticationPrincipal User user) {
        return designTemplateService.list(category, user);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ResponseEntity<DesignTemplateDto> create(@Valid @RequestBody DesignTemplateCreateRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designTemplateService.create(request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public DesignTemplateDto update(@PathVariable Long id,
                                    @Valid @RequestBody DesignTemplateUpdateRequest request,
                                    @AuthenticationPrincipal User user) {
        return designTemplateService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        designTemplateService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
