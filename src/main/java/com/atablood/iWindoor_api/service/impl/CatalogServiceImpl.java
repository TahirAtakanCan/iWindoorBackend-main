package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.catalog.*;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.ColorRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.HardwareRuleRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.atablood.iWindoor_api.service.CatalogProvisioningService;
import com.atablood.iWindoor_api.service.CatalogService;
import com.atablood.iWindoor_api.service.SalesPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private static final Set<String> HARDWARE_KINDS = Set.of(
            "TURN", "TILT", "TILT_TURN", "SLIDE", "PASSIVE", "DOOR", "DOOR_PASSIVE",
            "SLIDE_FIXED", "LIFT_SLIDE");
    private static final Set<String> QTY_TYPES = Set.of("PER_SASH", "PER_PERIMETER_METER");
    private static final Set<String> ADJUST_TARGETS = Set.of("PROFILE", "GLASS", "ACCESSORY", "ALL");

    private final SeriesRepository seriesRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;
    private final HardwareRuleRepository hardwareRuleRepository;
    private final ColorRepository colorRepository;
    private final CatalogProvisioningService provisioning;
    private final SalesPricingService salesPricingService;
    private final AccessGuard guard;

    private Long company(User user) {
        Long id = guard.companyId(user);
        provisioning.ensureCompanyCatalog(id);
        return id;
    }

    @Override
    @Transactional
    public CatalogSnapshotDto snapshot(User user) {
        Long c = company(user);
        return new CatalogSnapshotDto(
                Instant.now(),
                seriesRepository.findByCompanyIdOrderByIdAsc(c).stream().map(SeriesDto::of).toList(),
                profileRepository.findByCompanyIdOrderByIdAsc(c).stream().map(ProfileDto::of).toList(),
                glassRepository.findByCompanyIdOrderByIdAsc(c).stream().map(GlassDto::of).toList(),
                accessoryRepository.findByCompanyIdOrderByIdAsc(c).stream().map(AccessoryDto::of).toList(),
                colorRepository.findByCompanyIdOrderBySortOrderAscIdAsc(c).stream().map(ColorDto::of).toList());
    }

    // --- SERİ ---
    @Override
    @Transactional
    public List<Series> getAllSeries(User user) {
        return seriesRepository.findByCompanyIdOrderByIdAsc(company(user));
    }

    @Override
    @Transactional
    public SeriesDto createSeries(SeriesRequest r, User user) {
        Series s = new Series();
        s.setCompanyId(company(user));
        apply(s, r);
        return SeriesDto.of(seriesRepository.save(s));
    }

    @Override
    @Transactional
    public SeriesDto updateSeries(Long id, SeriesRequest r, User user) {
        Series s = seriesRepository.findById(id).orElseThrow(() -> new NotFoundException("Seri", id));
        guard.checkCatalogWrite(s.getCompanyId(), user);
        BigDecimal beforeM2 = s.getSalePricePerM2();
        apply(s, r);
        Series saved = seriesRepository.save(s);
        if (!sameMoney(beforeM2, saved.getSalePricePerM2()) && saved.getCompanyId() != null) {
            salesPricingService.recalculateSeriesSales(saved.getCompanyId(), saved.getId());
        }
        return SeriesDto.of(saved);
    }

    private static boolean sameMoney(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    private static void apply(Series s, SeriesRequest r) {
        s.setName(r.name());
        s.setBrand(r.brand());
        s.setSystemDepth(r.systemDepthMm());
        if (r.material() != null) s.setMaterial(r.material());
        s.setSalePricePerM2(r.salePricePerM2() == null ? null
                : r.salePricePerM2().setScale(2, RoundingMode.HALF_UP));
        SeriesGeometryRequest g = r.geometry();
        if (g == null) return;
        set(g.frameFace(), s::setFrameFaceMm);
        set(g.sashFace(), s::setSashFaceMm);
        set(g.mullionFace(), s::setMullionFaceMm);
        set(g.beadFace(), s::setBeadFaceMm);
        set(g.sashLap(), s::setSashLapMm);
        set(g.glassRebateFrame(), s::setGlassRebateFrameMm);
        set(g.glassRebateSash(), s::setGlassRebateSashMm);
        set(g.glassRebateMullion(), s::setGlassRebateMullionMm);
        set(g.weldAllowance(), s::setWeldAllowanceMm);
        set(g.mullionEndAllowance(), s::setMullionEndAllowanceMm);
        set(g.slideInterlock(), s::setSlideInterlockMm);
        set(g.duplexWidth(), s::setDuplexWidthMm);
        set(g.minSashWidth(), s::setMinSashWidthMm);
        set(g.minSashHeight(), s::setMinSashHeightMm);
        set(g.maxSashWidth(), s::setMaxSashWidthMm);
        set(g.maxSashHeight(), s::setMaxSashHeightMm);
        set(g.maxSlideSashWidth(), s::setMaxSlideSashWidthMm);
        set(g.maxLiftSlideSashWidth(), s::setMaxLiftSlideSashWidthMm);
        set(g.maxFixedGlassArea(), s::setMaxFixedGlassAreaM2);
        set(g.adapterFace(), s::setAdapterFaceMm);
        set(g.doorSashFace(), s::setDoorSashFaceMm);
        set(g.thresholdFace(), s::setThresholdFaceMm);
        set(g.thresholdEndAllowance(), s::setThresholdEndAllowanceMm);
        set(g.minDoorSashWidth(), s::setMinDoorSashWidthMm);
        set(g.maxDoorSashWidth(), s::setMaxDoorSashWidthMm);
        set(g.minDoorSashHeight(), s::setMinDoorSashHeightMm);
        set(g.maxDoorSashHeight(), s::setMaxDoorSashHeightMm);
        set(g.couplingFace(), s::setCouplingFaceMm);
        set(g.couplingGap(), s::setCouplingGapMm);
    }

    private static void set(Double v, Consumer<Double> setter) {
        if (v != null) setter.accept(v);
    }

    @Override
    public Series defaultSeries(Long companyId) {
        return seriesRepository.findFirstByCompanyIdAndActiveTrueOrderByIdAsc(companyId).orElse(null);
    }

    @Override
    public Series requireSeries(Long seriesId, User user) {
        Series s = seriesRepository.findById(seriesId).orElseThrow(() -> new NotFoundException("Seri", seriesId));
        guard.checkCatalogRead(s.getCompanyId(), user);
        return s;
    }

    // --- PROFİL ---
    @Override
    @Transactional
    public List<Profile> getAllProfiles(User user, boolean includeInactive) {
        Long c = company(user);
        return includeInactive
                ? profileRepository.findByCompanyIdOrderByIdAsc(c)
                : profileRepository.findByCompanyIdAndActiveTrueOrderByIdAsc(c);
    }

    @Override
    @Transactional
    public List<Profile> getProfilesBySeries(Long seriesId, User user) {
        company(user);
        requireSeries(seriesId, user);
        return profileRepository.findBySeries_IdOrderByIdAsc(seriesId);
    }

    @Override
    @Transactional
    public List<Profile> getProfilesByType(ProfileType type, User user) {
        return profileRepository.findByCompanyIdAndTypeOrderByIdAsc(company(user), type);
    }

    @Override
    @Transactional
    public ProfileDto createProfile(ProfileRequest r, User user) {
        Long c = company(user);
        Series series = seriesRepository.findById(r.seriesId()).orElseThrow(() -> new NotFoundException("Seri", r.seriesId()));
        guard.checkCatalogWrite(series.getCompanyId(), user);
        if (profileRepository.existsByCompanyIdAndCode(c, r.code())) {
            throw new ConflictException("CODE_TAKEN", "Bu stok kodu zaten var: " + r.code());
        }
        if (r.minGlassMm() != null && r.maxGlassMm() != null && r.minGlassMm() > r.maxGlassMm()) {
            throw new BadRequestException("INVALID_RANGE", "minGlassMm, maxGlassMm'den büyük olamaz.");
        }
        Profile p = new Profile();
        p.setSeries(series);
        p.setCompanyId(series.getCompanyId());
        p.setCode(r.code());
        p.setName(r.name());
        p.setType(r.type());
        p.setWidthMm(r.faceWidthMm());
        if (r.barLengthMm() != null) p.setBarLengthMm(r.barLengthMm());
        p.setMinGlassMm(r.minGlassMm());
        p.setMaxGlassMm(r.maxGlassMm());
        p.setDefaultForType(Boolean.TRUE.equals(r.isDefault()));
        p.setPricePerMeter(r.pricePerMeter());
        if (r.currency() != null) p.setCurrency(r.currency());
        if (p.isDefaultForType()) clearOtherDefaults(p);
        return ProfileDto.of(profileRepository.save(p));
    }

    @Override
    @Transactional
    public ProfileDto updateProfile(Long id, ProfileUpdateRequest r, User user) {
        requireNonNegative(r.pricePerMeter());
        Profile profile = profileRepository.findById(id).orElseThrow(() -> new NotFoundException("Profil", id));
        guard.checkCatalogWrite(profile.getCompanyId(), user);
        profile.setName(r.name());
        profile.setPricePerMeter(r.pricePerMeter());
        if (r.barLength() != null) profile.setBarLengthMm(r.barLength());
        if (r.isActive() != null) profile.setActive(r.isActive());
        if (r.isDefault() != null) {
            profile.setDefaultForType(r.isDefault());
            if (r.isDefault()) clearOtherDefaults(profile);
        }
        return ProfileDto.of(profileRepository.save(profile));
    }

    private void clearOtherDefaults(Profile profile) {
        if (profile.getSeries() == null || profile.getCompanyId() == null) return;
        for (Profile other : profileRepository.findBySeries_IdAndTypeAndCompanyId(
                profile.getSeries().getId(), profile.getType(), profile.getCompanyId())) {
            if (!other.getId().equals(profile.getId()) && other.isDefaultForType()) {
                other.setDefaultForType(false);
                profileRepository.save(other);
            }
        }
    }

    @Override
    @Transactional
    public void updateProfilePrice(Long id, BigDecimal price, User user) {
        requireNonNegative(price);
        Profile profile = profileRepository.findById(id).orElseThrow(() -> new NotFoundException("Profil", id));
        guard.checkCatalogWrite(profile.getCompanyId(), user);
        profile.setPricePerMeter(price);
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public ProfileDto findBead(Long seriesId, double t, User user) {
        company(user);
        requireSeries(seriesId, user);
        List<Profile> beads = profileRepository.findBySeries_IdAndTypeOrderByIdAsc(seriesId, ProfileType.BEAD);
        return beads.stream()
                .filter(b -> b.getMinGlassMm() != null && b.getMaxGlassMm() != null && t >= b.getMinGlassMm() && t <= b.getMaxGlassMm())
                .min(Comparator.comparing((Profile b) -> !b.isDefaultForType()).thenComparing(Profile::getId))
                .map(ProfileDto::of)
                .orElseThrow(() -> new NotFoundException("Bu cam kalınlığına uygun çıta", t + " mm"));
    }

    // --- CAM ---
    @Override
    @Transactional
    public List<Glass> getAllGlasses(User user, boolean includeInactive) {
        Long c = company(user);
        return includeInactive
                ? glassRepository.findByCompanyIdOrderByIdAsc(c)
                : glassRepository.findByCompanyIdAndIsActiveTrueOrderByIdAsc(c);
    }

    @Override
    @Transactional
    public GlassDto createGlass(GlassRequest r, User user) {
        Long c = company(user);
        requireNonNegative(r.pricePerSquareMeter());
        if (glassRepository.existsByCompanyIdAndCode(c, r.code())) {
            throw new ConflictException("CODE_TAKEN", "Bu cam kodu zaten var: " + r.code());
        }
        Glass g = new Glass();
        g.setCompanyId(c);
        g.setCode(r.code());
        g.setName(r.name());
        g.setThicknessMm(r.thicknessMm());
        if (r.visualStyle() != null) g.setVisualStyle(r.visualStyle());
        g.setPricePerSquareMeter(r.pricePerSquareMeter());
        if (r.currency() != null) g.setCurrency(r.currency());
        return GlassDto.of(glassRepository.save(g));
    }

    @Override
    @Transactional
    public GlassDto updateGlass(Long id, GlassUpdateRequest r, User user) {
        requireNonNegative(r.pricePerSquareMeter());
        Glass glass = glassRepository.findById(id).orElseThrow(() -> new NotFoundException("Cam", id));
        guard.checkCatalogWrite(glass.getCompanyId(), user);
        glass.setName(r.name());
        if (r.thicknessMm() != null) glass.setThicknessMm(r.thicknessMm());
        if (r.visualStyle() != null) glass.setVisualStyle(r.visualStyle());
        glass.setPricePerSquareMeter(r.pricePerSquareMeter());
        if (r.isActive() != null) glass.setActive(r.isActive());
        return GlassDto.of(glassRepository.save(glass));
    }

    @Override
    @Transactional
    public void updateGlassPrice(Long id, BigDecimal price, User user) {
        requireNonNegative(price);
        Glass glass = glassRepository.findById(id).orElseThrow(() -> new NotFoundException("Cam", id));
        guard.checkCatalogWrite(glass.getCompanyId(), user);
        glass.setPricePerSquareMeter(price);
        glassRepository.save(glass);
    }

    // --- AKSESUAR ---
    @Override
    @Transactional
    public List<AccessoryDto> getAccessories(User user, boolean includeInactive) {
        Long c = company(user);
        List<Accessory> list = includeInactive
                ? accessoryRepository.findByCompanyIdOrderByIdAsc(c)
                : accessoryRepository.findByCompanyIdAndActiveTrueOrderByIdAsc(c);
        return list.stream().map(AccessoryDto::of).toList();
    }

    @Override
    @Transactional
    public AccessoryDto createAccessory(AccessoryRequest r, User user) {
        Long c = company(user);
        requireNonNegative(r.price());
        if (accessoryRepository.existsByCompanyIdAndCode(c, r.code())) {
            throw new ConflictException("CODE_TAKEN", "Bu aksesuar kodu zaten var: " + r.code());
        }
        Accessory a = new Accessory();
        a.setCompanyId(c);
        a.setCode(r.code().trim());
        a.setName(r.name());
        a.setUnit(r.unit());
        a.setPrice(r.price());
        if (r.isActive() != null) a.setActive(r.isActive());
        return AccessoryDto.of(accessoryRepository.save(a));
    }

    @Override
    @Transactional
    public AccessoryDto updateAccessory(Long id, AccessoryRequest r, User user) {
        requireNonNegative(r.price());
        Accessory a = accessoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Aksesuar", id));
        guard.checkCatalogWrite(a.getCompanyId(), user);
        String newCode = r.code().trim();
        if (!newCode.equalsIgnoreCase(a.getCode()) && accessoryRepository.existsByCompanyIdAndCode(a.getCompanyId(), newCode)) {
            throw new ConflictException("CODE_TAKEN", "Bu aksesuar kodu zaten var: " + newCode);
        }
        a.setCode(newCode);
        a.setName(r.name());
        a.setUnit(r.unit());
        a.setPrice(r.price());
        if (r.isActive() != null) a.setActive(r.isActive());
        return AccessoryDto.of(accessoryRepository.save(a));
    }

    // --- RENK ---
    @Override
    @Transactional
    public List<ColorDto> getColors(User user, boolean includeInactive) {
        Long c = company(user);
        List<Color> list = includeInactive
                ? colorRepository.findByCompanyIdOrderBySortOrderAscIdAsc(c)
                : colorRepository.findByCompanyIdAndActiveTrueOrderBySortOrderAscIdAsc(c);
        return list.stream().map(ColorDto::of).toList();
    }

    @Override
    @Transactional
    public ColorDto createColor(ColorRequest r, User user) {
        Long c = company(user);
        guard.checkCatalogWrite(c, user);
        if (colorRepository.existsByCompanyIdAndCode(c, r.code())) {
            throw new ConflictException("CODE_TAKEN", "Bu renk kodu zaten var: " + r.code());
        }
        Color color = new Color();
        color.setCompanyId(c);
        applyColor(color, r);
        return ColorDto.of(colorRepository.save(color));
    }

    @Override
    @Transactional
    public ColorDto updateColor(Long id, ColorRequest r, User user) {
        Color color = colorRepository.findById(id).orElseThrow(() -> new NotFoundException("Renk", id));
        guard.checkCatalogWrite(color.getCompanyId(), user);
        String newCode = r.code().trim();
        if (!newCode.equalsIgnoreCase(color.getCode())
                && colorRepository.existsByCompanyIdAndCode(color.getCompanyId(), newCode)) {
            throw new ConflictException("CODE_TAKEN", "Bu renk kodu zaten var: " + newCode);
        }
        applyColor(color, r);
        return ColorDto.of(colorRepository.save(color));
    }

    private static void applyColor(Color color, ColorRequest r) {
        color.setCode(r.code().trim());
        color.setName(r.name());
        color.setHexOutside(r.hexOutside());
        color.setHexInside(r.hexInside());
        color.setSurchargePct(r.surchargePct());
        if (r.isActive() != null) color.setActive(r.isActive());
        if (r.sortOrder() != null) color.setSortOrder(r.sortOrder());
    }

    // --- DONANIM ---
    @Override
    @Transactional
    public List<HardwareRuleDto> getHardwareRules(User user) {
        Long c = company(user);
        return hardwareRuleRepository.findByCompanyIdOrderBySortOrderAscIdAsc(c).stream()
                .map(rule -> toHardwareDto(c, rule))
                .toList();
    }

    @Override
    @Transactional
    public HardwareRuleDto createHardwareRule(HardwareRuleRequest r, User user) {
        Long c = company(user);
        validateHardwareRequest(r, c, user);
        HardwareRule rule = new HardwareRule();
        applyHardware(rule, r, c);
        return toHardwareDto(c, hardwareRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public HardwareRuleDto updateHardwareRule(Long id, HardwareRuleRequest r, User user) {
        HardwareRule rule = hardwareRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Donanım kuralı", id));
        guard.checkCatalogWrite(rule.getCompanyId(), user);
        validateHardwareRequest(r, rule.getCompanyId(), user);
        applyHardware(rule, r, rule.getCompanyId());
        return toHardwareDto(rule.getCompanyId(), hardwareRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public void deleteHardwareRule(Long id, User user) {
        HardwareRule rule = hardwareRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Donanım kuralı", id));
        guard.checkCatalogWrite(rule.getCompanyId(), user);
        hardwareRuleRepository.delete(rule);
    }

    private void validateHardwareRequest(HardwareRuleRequest r, Long companyId, User user) {
        if (!HARDWARE_KINDS.contains(r.kind())) {
            throw new BadRequestException("VALIDATION_FAILED", "kind TURN/TILT/TILT_TURN/SLIDE olmalı.");
        }
        if (!QTY_TYPES.contains(r.qtyType())) {
            throw new BadRequestException("VALIDATION_FAILED", "qtyType PER_SASH veya PER_PERIMETER_METER olmalı.");
        }
        if (r.qty() == null || r.qty().signum() <= 0) {
            throw new BadRequestException("VALIDATION_FAILED", "qty sıfırdan büyük olmalı.");
        }
        if (!accessoryRepository.existsByCompanyIdAndCode(companyId, r.accessoryCode())) {
            throw new BadRequestException("VALIDATION_FAILED", "Aksesuar bulunamadı: " + r.accessoryCode());
        }
        if (r.seriesId() != null) {
            Series s = seriesRepository.findById(r.seriesId())
                    .orElseThrow(() -> new NotFoundException("Seri", r.seriesId()));
            guard.checkCatalogWrite(s.getCompanyId(), user);
            if (!companyId.equals(s.getCompanyId())) {
                throw new BadRequestException("VALIDATION_FAILED", "Seri bu şirkete ait değil.");
            }
        }
    }

    private static void applyHardware(HardwareRule rule, HardwareRuleRequest r, Long companyId) {
        rule.setCompanyId(companyId);
        rule.setKind(r.kind());
        rule.setSeriesId(r.seriesId());
        rule.setAccessoryCode(r.accessoryCode());
        rule.setQtyType(r.qtyType());
        rule.setQty(r.qty());
        if (r.sortOrder() != null) rule.setSortOrder(r.sortOrder());
    }

    private HardwareRuleDto toHardwareDto(Long companyId, HardwareRule rule) {
        String name = accessoryRepository.findFirstByCompanyIdAndCode(companyId, rule.getAccessoryCode())
                .map(Accessory::getName)
                .orElse(rule.getAccessoryCode());
        return new HardwareRuleDto(rule.getId(), rule.getKind(), rule.getSeriesId(), rule.getAccessoryCode(),
                name, rule.getQtyType(), rule.getQty());
    }

    // --- TOPLU ZAM ---
    @Override
    @Transactional
    public PriceAdjustmentResponse adjustPrices(PriceAdjustmentRequest request, boolean dryRun, User user) {
        Long c = company(user);
        String target = request.target() == null ? "" : request.target().trim().toUpperCase();
        if (!ADJUST_TARGETS.contains(target)) {
            throw new BadRequestException("VALIDATION_FAILED", "target PROFILE|GLASS|ACCESSORY|ALL olmalı.");
        }
        BigDecimal percent = request.percent();
        if (percent.compareTo(new BigDecimal("-50")) < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("VALIDATION_FAILED", "percent -50 ile 100 arasında olmalı.");
        }
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        int updated = 0;

        if ("PROFILE".equals(target) || "ALL".equals(target)) {
            for (Profile p : profileRepository.findByCompanyIdAndActiveTrueOrderByIdAsc(c)) {
                updated++;
                if (!dryRun) {
                    p.setPricePerMeter(money(p.getPricePerMeter().multiply(factor)));
                    profileRepository.save(p);
                }
            }
        }
        if ("GLASS".equals(target) || "ALL".equals(target)) {
            for (Glass g : glassRepository.findByCompanyIdAndIsActiveTrueOrderByIdAsc(c)) {
                updated++;
                if (!dryRun) {
                    g.setPricePerSquareMeter(money(g.getPricePerSquareMeter().multiply(factor)));
                    glassRepository.save(g);
                }
            }
        }
        if ("ACCESSORY".equals(target) || "ALL".equals(target)) {
            for (Accessory a : accessoryRepository.findByCompanyIdAndActiveTrueOrderByIdAsc(c)) {
                updated++;
                if (!dryRun) {
                    a.setPrice(money(a.getPrice().multiply(factor)));
                    accessoryRepository.save(a);
                }
            }
        }
        return new PriceAdjustmentResponse(updated);
    }

    private static void requireNonNegative(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new BadRequestException("INVALID_PRICE", "Fiyat negatif olamaz.");
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
