package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.dashboard.DashboardDto;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDto get(@AuthenticationPrincipal User user) {
        return dashboardService.get(user);
    }
}
