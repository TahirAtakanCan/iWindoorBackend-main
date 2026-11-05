package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.dto.catalog.*;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public SeriesDto createSeries(@Valid @RequestBody SeriesRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createSeries(request, user);
    }

    @PutMapping("/series/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
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

    // --- PROFİL ---
    @PostMapping("/profiles")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ProfileDto createProfile(@Valid @RequestBody ProfileRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createProfile(request, user);
    }

    @GetMapping("/profiles")
    public List<Profile> getAllProfiles(@RequestParam(defaultValue = "false") boolean includeInactive,
                                        @AuthenticationPrincipal User user) {
        return catalogService.getAllProfiles(user, includeInactive);
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
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ResponseEntity<Void> updateProfilePrice(@PathVariable Long id, @RequestParam BigDecimal price,
                                                   @AuthenticationPrincipal User user) {
        catalogService.updateProfilePrice(id, price, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ProfileDto updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest request,
                                    @AuthenticationPrincipal User user) {
        return catalogService.updateProfile(id, request, user);
    }

    // --- CAM ---
    @PostMapping("/glasses")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public GlassDto createGlass(@Valid @RequestBody GlassRequest request, @AuthenticationPrincipal User user) {
        return catalogService.createGlass(request, user);
    }

    @GetMapping("/glasses")
    public List<Glass> getAllGlasses(@RequestParam(defaultValue = "false") boolean includeInactive,
                                     @AuthenticationPrincipal User user) {
        return catalogService.getAllGlasses(user, includeInactive);
    }

    @PutMapping("/glasses/{id}/price")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ResponseEntity<Void> updateGlassPrice(@PathVariable Long id, @RequestParam BigDecimal price,
                                                 @AuthenticationPrincipal User user) {
        catalogService.updateGlassPrice(id, price, user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/glasses/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public GlassDto updateGlass(@PathVariable Long id, @Valid @RequestBody GlassUpdateRequest request,
                                @AuthenticationPrincipal User user) {
        return catalogService.updateGlass(id, request, user);
    }

    // --- AKSESUAR ---
    @GetMapping("/accessories")
    public List<AccessoryDto> getAccessories(@RequestParam(defaultValue = "false") boolean includeInactive,
                                             @AuthenticationPrincipal User user) {
        return catalogService.getAccessories(user, includeInactive);
    }

    @PostMapping("/accessories")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public AccessoryDto createAccessory(@Valid @RequestBody AccessoryRequest request,
                                        @AuthenticationPrincipal User user) {
        return catalogService.createAccessory(request, user);
    }

    @PutMapping("/accessories/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public AccessoryDto updateAccessory(@PathVariable Long id, @Valid @RequestBody AccessoryRequest request,
                                        @AuthenticationPrincipal User user) {
        return catalogService.updateAccessory(id, request, user);
    }

    // --- DONANIM KURALLARI ---
    @GetMapping("/hardware-rules")
    public List<HardwareRuleDto> hardwareRules(@AuthenticationPrincipal User user) {
        return catalogService.getHardwareRules(user);
    }

    @PostMapping("/hardware-rules")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public HardwareRuleDto createHardwareRule(@Valid @RequestBody HardwareRuleRequest request,
                                              @AuthenticationPrincipal User user) {
        return catalogService.createHardwareRule(request, user);
    }

    @PutMapping("/hardware-rules/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public HardwareRuleDto updateHardwareRule(@PathVariable Long id, @Valid @RequestBody HardwareRuleRequest request,
                                              @AuthenticationPrincipal User user) {
        return catalogService.updateHardwareRule(id, request, user);
    }

    @DeleteMapping("/hardware-rules/{id}")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteHardwareRule(@PathVariable Long id, @AuthenticationPrincipal User user) {
        catalogService.deleteHardwareRule(id, user);
        return ResponseEntity.ok().build();
    }

    // --- TOPLU ZAM ---
    @PostMapping("/price-adjustment")
    @PreAuthorize("hasAnyAuthority('COMPANY_ADMIN','ADMIN')")
    public PriceAdjustmentResponse adjustPrices(@Valid @RequestBody PriceAdjustmentRequest request,
                                                @RequestParam(defaultValue = "false") boolean dryRun,
                                                @AuthenticationPrincipal User user) {
        return catalogService.adjustPrices(request, dryRun, user);
    }
}
