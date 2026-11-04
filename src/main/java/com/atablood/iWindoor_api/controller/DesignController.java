package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * ESKİ düğüm tabanlı editör uçları (mevcut mobil uygulama kullanıyor).
 * Yeni çizim motoru /api/v1/windows/{id} (tasarım belgesi) kullanır; bu uçlar geçiş bitince kaldırılacak.
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/design")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @PostMapping("/split/{nodeId}")
    public WindowNode splitNode(@PathVariable Long nodeId, @RequestParam boolean isVertical,
                                @AuthenticationPrincipal User user) {
        return designService.splitNode(nodeId, isVertical, user);
    }

    @PostMapping("/update-type/{nodeId}")
    public WindowNode updateNodeType(@PathVariable Long nodeId, @RequestParam String type,
                                     @AuthenticationPrincipal User user) {
        return designService.updateNodeType(nodeId, type, user);
    }

    @PostMapping("/assign-material/{nodeId}")
    public WindowNode assignMaterial(@PathVariable Long nodeId, @RequestParam Long materialId,
                                     @RequestParam String type, @AuthenticationPrincipal User user) {
        return designService.assignMaterial(nodeId, materialId, type, user);
    }
}
