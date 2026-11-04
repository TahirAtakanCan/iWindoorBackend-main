package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Her şirketin kendi kataloğu (seri, profil, cam, aksesuar ve fiyatları) olur.
 * Şirketin hiç serisi yoksa şablon katalog (company_id IS NULL) kopyalanır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogProvisioningService {

    private final CompanyRepository companyRepository;
    private final SeriesRepository seriesRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;

    @Transactional
    public void ensureCompanyCatalog(Long companyId) {
        if (companyId == null || seriesRepository.existsByCompanyId(companyId)) return;
        companyRepository.lockById(companyId).orElseThrow();
        if (seriesRepository.existsByCompanyId(companyId)) return; // kilidi bekleyen ikinci istek

        Map<Long, Series> copies = new HashMap<>();
        for (Series t : seriesRepository.findByCompanyIdIsNullOrderByIdAsc()) {
            Series s = new Series();
            s.setName(t.getName());
            s.setBrand(t.getBrand());
            s.setSystemDepth(t.getSystemDepth());
            s.setMaterial(t.getMaterial());
            s.setActive(t.isActive());
            s.setFrameFaceMm(t.getFrameFaceMm());
            s.setSashFaceMm(t.getSashFaceMm());
            s.setMullionFaceMm(t.getMullionFaceMm());
            s.setBeadFaceMm(t.getBeadFaceMm());
            s.setSashLapMm(t.getSashLapMm());
            s.setGlassRebateFrameMm(t.getGlassRebateFrameMm());
            s.setGlassRebateSashMm(t.getGlassRebateSashMm());
            s.setGlassRebateMullionMm(t.getGlassRebateMullionMm());
            s.setWeldAllowanceMm(t.getWeldAllowanceMm());
            s.setMullionEndAllowanceMm(t.getMullionEndAllowanceMm());
            s.setSlideInterlockMm(t.getSlideInterlockMm());
            s.setDuplexWidthMm(t.getDuplexWidthMm());
            s.setCompanyId(companyId);
            copies.put(t.getId(), seriesRepository.save(s));
        }
        for (Profile t : profileRepository.findByCompanyIdIsNullOrderByIdAsc()) {
            Series target = t.getSeries() == null ? null : copies.get(t.getSeries().getId());
            if (target == null) continue;
            Profile p = new Profile();
            p.setCode(t.getCode());
            p.setName(t.getName());
            p.setType(t.getType());
            p.setWidthMm(t.getWidthMm());
            p.setWeightPerMeter(t.getWeightPerMeter());
            p.setPricePerMeter(t.getPricePerMeter());
            p.setCurrency(t.getCurrency());
            p.setBarLengthMm(t.getBarLengthMm());
            p.setMinGlassMm(t.getMinGlassMm());
            p.setMaxGlassMm(t.getMaxGlassMm());
            p.setDefaultForType(t.isDefaultForType());
            p.setActive(t.isActive());
            p.setSeries(target);
            p.setCompanyId(companyId);
            profileRepository.save(p);
        }
        for (Glass t : glassRepository.findByCompanyIdIsNullOrderByIdAsc()) {
            Glass g = new Glass();
            g.setCode(t.getCode());
            g.setName(t.getName());
            g.setThicknessMm(t.getThicknessMm());
            g.setVisualStyle(t.getVisualStyle());
            g.setPricePerSquareMeter(t.getPricePerSquareMeter());
            g.setCurrency(t.getCurrency());
            g.setActive(t.isActive());
            g.setCompanyId(companyId);
            glassRepository.save(g);
        }
        for (Accessory t : accessoryRepository.findByCompanyIdIsNullOrderByIdAsc()) {
            Accessory a = new Accessory();
            a.setCode(t.getCode());
            a.setName(t.getName());
            a.setUnit(t.getUnit());
            a.setPrice(t.getPrice());
            a.setCurrency(t.getCurrency());
            a.setCompanyId(companyId);
            accessoryRepository.save(a);
        }
        log.info("Şirket {} için şablon katalog kopyalandı ({} seri).", companyId, copies.size());
    }
}
