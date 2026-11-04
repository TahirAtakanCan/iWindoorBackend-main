package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.company.CompanyDto;
import com.atablood.iWindoor_api.dto.company.CompanyUpdateRequest;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public CompanyDto get(@AuthenticationPrincipal User currentUser) {
        return companyService.getMine(currentUser);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public CompanyDto update(@Valid @RequestBody CompanyUpdateRequest request,
                             @AuthenticationPrincipal User currentUser) {
        return companyService.updateMine(request, currentUser);
    }
}
