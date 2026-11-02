package com.atablood.iWindoor_api.controller;

import com.atablood.iWindoor_api.entity.Glass;
import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType;
import com.atablood.iWindoor_api.entity.Series;
import com.atablood.iWindoor_api.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    // --- SERİ İŞLEMLERİ ---

    @PostMapping("/series")
    public ResponseEntity<Series> createSeries(@RequestBody Series series) {
        return ResponseEntity.ok(catalogService.createSeries(series));
    }

    @GetMapping("/series")
    public ResponseEntity<List<Series>> getAllSeries() {
        return ResponseEntity.ok(catalogService.getAllSeries());
    }

    // --- PROFİL İŞLEMLERİ ---

    @PostMapping("/profiles")
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile) {
        return ResponseEntity.ok(catalogService.createProfile(profile));
    }

    // Tüm profilleri getiren endpoint (Ayarlar ekranı için gerekli olabilir)
    @GetMapping("/profiles")
    public ResponseEntity<List<Profile>> getAllProfiles() {
        return ResponseEntity.ok(catalogService.getAllProfiles());
    }

    @GetMapping("/profiles/by-series/{seriesId}")
    public ResponseEntity<List<Profile>> getProfilesBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(catalogService.getProfilesBySeries(seriesId));
    }

    @GetMapping("/profiles/filter")
    public ResponseEntity<List<Profile>> getProfilesByType(@RequestParam ProfileType type) {
        return ResponseEntity.ok(catalogService.getProfilesByType(type));
    }

    // Profil Fiyatı Güncelle
    @PutMapping("/profiles/{id}/price")
    public ResponseEntity<Void> updateProfilePrice(@PathVariable Long id, @RequestParam java.math.BigDecimal price) {
        catalogService.updateProfilePrice(id, price);
        return ResponseEntity.ok().build();
    }

    // --- CAM İŞLEMLERİ ---

    @PostMapping("/glasses")
    public ResponseEntity<Glass> createGlass(@RequestBody Glass glass) {
        return ResponseEntity.ok(catalogService.createGlass(glass));
    }

    @GetMapping("/glasses")
    public ResponseEntity<List<Glass>> getAllGlasses() {
        return ResponseEntity.ok(catalogService.getAllGlasses());
    }

    // Cam Fiyatı Güncelle (404 Hatasını Çözen Kısım)
    @PutMapping("/glasses/{id}/price")
    public ResponseEntity<Void> updateGlassPrice(@PathVariable Long id, @RequestParam java.math.BigDecimal price) {
        catalogService.updateGlassPrice(id, price);
        return ResponseEntity.ok().build();
    }
}