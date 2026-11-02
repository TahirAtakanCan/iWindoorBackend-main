package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.entity.Glass;
import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType;
import com.atablood.iWindoor_api.entity.Series;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import com.atablood.iWindoor_api.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final SeriesRepository seriesRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;

    // --- SERİ ---
    @Override
    public Series createSeries(Series series) {
        return seriesRepository.save(series);
    }

    @Override
    public List<Series> getAllSeries() {
        return seriesRepository.findAll();
    }

    // --- PROFİL ---
    @Override
    public Profile createProfile(Profile profile) {
        return profileRepository.save(profile);
    }

    @Override
    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    @Override
    public List<Profile> getProfilesBySeries(Long seriesId) {
        return profileRepository.findBySeriesId(seriesId);
    }

    @Override
    public List<Profile> getProfilesByType(ProfileType type) {
        return profileRepository.findByType(type);
    }

    @Override
    public void updateProfilePrice(Long id, java.math.BigDecimal newPrice) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profil bulunamadı: " + id));
        profile.setPricePerMeter(newPrice);
        profileRepository.save(profile);
    }

    // --- CAM ---
    @Override
    public Glass createGlass(Glass glass) {
        return glassRepository.save(glass);
    }

    @Override
    public List<Glass> getAllGlasses() {
        return glassRepository.findAll();
    }

    @Override
    public void updateGlassPrice(Long id, java.math.BigDecimal newPrice) {
        Glass glass = glassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cam bulunamadı: " + id));
        glass.setPricePerSquareMeter(newPrice);
        glassRepository.save(glass);
    }
}