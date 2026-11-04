package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.catalog.*;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Katalog her zaman giriş yapan kullanıcının şirketine aittir. */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /** Çevrimdışı çizim için tek paket: seriler (geometri dahil), profiller, camlar, aksesuarlar. */
    @GetMapping("/snapshot")
    public CatalogSnapshotDto snapshot(@AuthenticationPrincipal User user) {
        return catalogService.snapshot(user);
    }

    // --- SERİ ---
    @GetMapping("/series")
    public List<SeriesDto> getAllSeries(@AuthenticationPrincipal User user) {
        return catalogService.getAllSeries(user).stream().map(SeriesDto::of).toList();
    }

    @PostMapping("/series")
    public SeriesDto createSeries(@Valid @RequestBody SeriesRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createSeries(request, user);
    }

    @PutMapping("/series/{id}")
    public SeriesDto updateSeries(@PathVariable Long id, @Valid @RequestBody SeriesRequest request,
                                  @AuthenticationPrincipal User user) {
        return catalogService.updateSeries(id, request, user);
    }

    /** Cam kalınlığına uygun çıta: GET /catalog/series/5/bead?glassThicknessMm=24 */
    @GetMapping("/series/{id}/bead")
    public ProfileDto findBead(@PathVariable Long id, @RequestParam double glassThicknessMm,
                               @AuthenticationPrincipal User user) {
        return catalogService.findBead(id, glassThicknessMm, user);
    }

    // --- PROFİL (mobil uygulama bu uçların entity biçimini kullanıyor) ---
    @PostMapping("/profiles")
    public ProfileDto createProfile(@Valid @RequestBody ProfileRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createProfile(request, user);
    }

    @GetMapping("/profiles")
    public List<Profile> getAllProfiles(@AuthenticationPrincipal User user) {
        return catalogService.getAllProfiles(user);
    }

    @GetMapping("/profiles/by-series/{seriesId}")
    public List<Profile> getProfilesBySeries(@PathVariable Long seriesId, @AuthenticationPrincipal User user) {
        return catalogService.getProfilesBySeries(seriesId, user);
    }

    @GetMapping("/profiles/filter")
    public List<Profile> getProfilesByType(@RequestParam ProfileType type, @AuthenticationPrincipal User user) {
        return catalogService.getProfilesByType(type, user);
    }

    @PutMapping("/profiles/{id}/price")
    public ResponseEntity<Void> updateProfilePrice(@PathVariable Long id, @RequestParam BigDecimal price,
                                                   @AuthenticationPrincipal User user) {
        catalogService.updateProfilePrice(id, price, user);
        return ResponseEntity.ok().build();
    }

    // --- CAM ---
    @PostMapping("/glasses")
    public GlassDto createGlass(@Valid @RequestBody GlassRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createGlass(request, user);
    }

    @GetMapping("/glasses")
    public List<Glass> getAllGlasses(@AuthenticationPrincipal User user) {
        return catalogService.getAllGlasses(user);
    }

    @PutMapping("/glasses/{id}/price")
    public ResponseEntity<Void> updateGlassPrice(@PathVariable Long id, @RequestParam BigDecimal price,
                                                 @AuthenticationPrincipal User user) {
        catalogService.updateGlassPrice(id, price, user);
        return ResponseEntity.ok().build();
    }
}
