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
 * Her şirketin kendi kataloğu (seri, profil, cam, aksesuar, donanım kuralları) olur.
 * Şirketin hiç serisi yoksa şablon katalog (company_id IS NULL) kopyalanır;
 * her çağrıda eksik şablon aksesuar/kuralları idempotent tamamlanır.
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
    private final HardwareRuleRepository hardwareRuleRepository;
    private final ColorRepository colorRepository;

    @Transactional
    public void ensureCompanyCatalog(Long companyId) {
        if (companyId == null) return;
        companyRepository.lockById(companyId).orElseThrow();

        if (!seriesRepository.existsByCompanyId(companyId)) {
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
                s.setMinSashWidthMm(t.getMinSashWidthMm());
                s.setMinSashHeightMm(t.getMinSashHeightMm());
                s.setMaxSashWidthMm(t.getMaxSashWidthMm());
                s.setMaxSashHeightMm(t.getMaxSashHeightMm());
                s.setMaxSlideSashWidthMm(t.getMaxSlideSashWidthMm());
                s.setMaxLiftSlideSashWidthMm(t.getMaxLiftSlideSashWidthMm());
                s.setMaxFixedGlassAreaM2(t.getMaxFixedGlassAreaM2());
                s.setAdapterFaceMm(t.getAdapterFaceMm());
                s.setDoorSashFaceMm(t.getDoorSashFaceMm());
                s.setThresholdFaceMm(t.getThresholdFaceMm());
                s.setThresholdEndAllowanceMm(t.getThresholdEndAllowanceMm());
                s.setMinDoorSashWidthMm(t.getMinDoorSashWidthMm());
                s.setMaxDoorSashWidthMm(t.getMaxDoorSashWidthMm());
                s.setMinDoorSashHeightMm(t.getMinDoorSashHeightMm());
                s.setMaxDoorSashHeightMm(t.getMaxDoorSashHeightMm());
                s.setCouplingFaceMm(t.getCouplingFaceMm());
                s.setCouplingGapMm(t.getCouplingGapMm());
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
                copyAccessory(t, companyId);
            }
            for (Color t : colorRepository.findByCompanyIdIsNullOrderBySortOrderAscIdAsc()) {
                copyColor(t, companyId);
            }
            for (HardwareRule t : hardwareRuleRepository.findByCompanyIdIsNullOrderBySortOrderAscIdAsc()) {
                Long mappedSeriesId = t.getSeriesId() == null ? null
                        : (copies.containsKey(t.getSeriesId()) ? copies.get(t.getSeriesId()).getId() : null);
                if (t.getSeriesId() != null && mappedSeriesId == null) continue;
                copyRule(t, companyId, mappedSeriesId);
            }
            log.info("Şirket {} için şablon katalog kopyalandı ({} seri).", companyId, copies.size());
        }

        syncTemplateAdditions(companyId);
    }

    /**
     * Eski şirketlere sonradan eklenen şablon aksesuarları tamamlar.
     * Donanım kuralları burada yeniden eklenmez: şirket admin silince (katalog yönetimi)
     * satır kalıcı olarak kaldırılır; sync ile geri getirilmemelidir.
     * Yeni şirketler ilk kopyada tüm şablon kurallarını alır.
     */
    public void syncTemplateAdditions(Long companyId) {
        for (Accessory t : accessoryRepository.findByCompanyIdIsNullOrderByIdAsc()) {
            if (!accessoryRepository.existsByCompanyIdAndCode(companyId, t.getCode())) {
                copyAccessory(t, companyId);
            }
        }
        for (Color t : colorRepository.findByCompanyIdIsNullOrderBySortOrderAscIdAsc()) {
            if (!colorRepository.existsByCompanyIdAndCode(companyId, t.getCode())) {
                copyColor(t, companyId);
            }
        }
    }

    private void copyAccessory(Accessory t, Long companyId) {
        Accessory a = new Accessory();
        a.setCode(t.getCode());
        a.setName(t.getName());
        a.setUnit(t.getUnit());
        a.setPrice(t.getPrice());
        a.setCurrency(t.getCurrency());
        a.setCompanyId(companyId);
        accessoryRepository.save(a);
    }

    private void copyColor(Color t, Long companyId) {
        Color c = new Color();
        c.setCompanyId(companyId);
        c.setCode(t.getCode());
        c.setName(t.getName());
        c.setHexOutside(t.getHexOutside());
        c.setHexInside(t.getHexInside());
        c.setSurchargePct(t.getSurchargePct());
        c.setActive(t.isActive());
        c.setSortOrder(t.getSortOrder());
        colorRepository.save(c);
    }

    private void copyRule(HardwareRule t, Long companyId, Long seriesId) {
        HardwareRule r = new HardwareRule();
        r.setCompanyId(companyId);
        r.setSeriesId(seriesId);
        r.setKind(t.getKind());
        r.setAccessoryCode(t.getAccessoryCode());
        r.setQtyType(t.getQtyType());
        r.setQty(t.getQty());
        r.setSortOrder(t.getSortOrder());
        hardwareRuleRepository.save(r);
    }
}
