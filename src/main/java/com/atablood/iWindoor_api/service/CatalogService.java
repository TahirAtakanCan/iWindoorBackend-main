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
    List<Profile> getAllProfiles(User user);

    List<Profile> getProfilesBySeries(Long seriesId, User user);

    List<Profile> getProfilesByType(ProfileType type, User user);

    ProfileDto createProfile(ProfileRequest request, User user);

    void updateProfilePrice(Long id, BigDecimal price, User user);

    ProfileDto findBead(Long seriesId, double glassThicknessMm, User user);

    // --- Cam ---
    List<Glass> getAllGlasses(User user);

    GlassDto createGlass(GlassRequest request, User user);

    void updateGlassPrice(Long id, BigDecimal price, User user);

    /** Şirketin ilk aktif serisi (yeni pencerelerin varsayılanı). */
    Series defaultSeries(Long companyId);

    Series requireSeries(Long seriesId, User user);
}
