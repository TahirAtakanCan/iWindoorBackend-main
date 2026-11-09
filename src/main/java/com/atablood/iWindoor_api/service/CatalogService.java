package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.catalog.*;
import com.atablood.iWindoor_api.entity.*;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogService {

    CatalogSnapshotDto snapshot(User user);

    // --- Seri ---
    List<Series> getAllSeries(User user);

    SeriesDto createSeries(SeriesRequest request, User user);

    SeriesDto updateSeries(Long id, SeriesRequest request, User user);

    // --- Profil ---
    List<Profile> getAllProfiles(User user, boolean includeInactive);

    List<Profile> getProfilesBySeries(Long seriesId, User user);

    List<Profile> getProfilesByType(ProfileType type, User user);

    ProfileDto createProfile(ProfileRequest request, User user);

    ProfileDto updateProfile(Long id, ProfileUpdateRequest request, User user);

    void updateProfilePrice(Long id, BigDecimal price, User user);

    ProfileDto findBead(Long seriesId, double glassThicknessMm, User user);

    // --- Cam ---
    List<Glass> getAllGlasses(User user, boolean includeInactive);

    GlassDto createGlass(GlassRequest request, User user);

    GlassDto updateGlass(Long id, GlassUpdateRequest request, User user);

    void updateGlassPrice(Long id, BigDecimal price, User user);

    // --- Aksesuar ---
    List<AccessoryDto> getAccessories(User user, boolean includeInactive);

    AccessoryDto createAccessory(AccessoryRequest request, User user);

    AccessoryDto updateAccessory(Long id, AccessoryRequest request, User user);

    // --- Renk ---
    List<ColorDto> getColors(User user, boolean includeInactive);

    ColorDto createColor(ColorRequest request, User user);

    ColorDto updateColor(Long id, ColorRequest request, User user);

    // --- Donanım kuralları ---
    List<HardwareRuleDto> getHardwareRules(User user);

    HardwareRuleDto createHardwareRule(HardwareRuleRequest request, User user);

    HardwareRuleDto updateHardwareRule(Long id, HardwareRuleRequest request, User user);

    void deleteHardwareRule(Long id, User user);

    // --- Toplu zam ---
    PriceAdjustmentResponse adjustPrices(PriceAdjustmentRequest request, boolean dryRun, User user);

    /** Şirketin ilk aktif serisi (yeni pencerelerin varsayılanı). */
    Series defaultSeries(Long companyId);

    Series requireSeries(Long seriesId, User user);
}
