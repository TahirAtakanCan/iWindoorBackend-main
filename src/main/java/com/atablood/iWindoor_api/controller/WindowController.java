package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.window.QuoteDto;
import com.atablood.iWindoor_api.dto.window.WindowDto;
import com.atablood.iWindoor_api.dto.window.WindowUpdateRequest;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.service.WindowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Tasarım belgesi tabanlı pencere API'si (yeni çizim motoru bunu kullanır). */
@RestController
@RequestMapping("/api/v1/windows")
@RequiredArgsConstructor
public class WindowController {

    private final WindowService windowService;

    @GetMapping("/{id}")
    public WindowDto get(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return windowService.get(id, user);
    }

    @PutMapping("/{id}")
    public WindowDto update(@PathVariable Long id, @Valid @RequestBody WindowUpdateRequest request,
                            @AuthenticationPrincipal User user) {
        return windowService.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        windowService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/quote")
    public QuoteDto quote(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return windowService.quote(id, user);
    }
}
