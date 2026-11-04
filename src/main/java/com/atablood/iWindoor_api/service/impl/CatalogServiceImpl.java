package com.atablood.iWindoor_api.service.impl;

import com.atablood.iWindoor_api.dto.catalog.*;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.atablood.iWindoor_api.service.CatalogProvisioningService;
import com.atablood.iWindoor_api.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final SeriesRepository seriesRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;
    private final CatalogProvisioningService provisioning;
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
                accessoryRepository.findByCompanyIdOrderByIdAsc(c).stream().map(AccessoryDto::of).toList());
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
        apply(s, r);
        return SeriesDto.of(seriesRepository.save(s));
    }

    private static void apply(Series s, SeriesRequest r) {
        s.setName(r.name());
        s.setBrand(r.brand());
        s.setSystemDepth(r.systemDepthMm());
        if (r.material() != null) s.setMaterial(r.material());
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
    public List<Profile> getAllProfiles(User user) {
        return profileRepository.findByCompanyIdOrderByIdAsc(company(user));
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
            throw new ConflictException("DUPLICATE_CODE", "Bu stok kodu zaten var: " + r.code());
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
        return ProfileDto.of(profileRepository.save(p));
    }

    @Override
    @Transactional
    public void updateProfilePrice(Long id, BigDecimal price, User user) {
        if (price == null || price.signum() < 0) throw new BadRequestException("INVALID_PRICE", "Fiyat negatif olamaz.");
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
    public List<Glass> getAllGlasses(User user) {
        return glassRepository.findByCompanyIdOrderByIdAsc(company(user));
    }

    @Override
    @Transactional
    public GlassDto createGlass(GlassRequest r, User user) {
        Long c = company(user);
        if (glassRepository.existsByCompanyIdAndCode(c, r.code())) {
            throw new ConflictException("DUPLICATE_CODE", "Bu cam kodu zaten var: " + r.code());
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
    public void updateGlassPrice(Long id, BigDecimal price, User user) {
        if (price == null || price.signum() < 0) throw new BadRequestException("INVALID_PRICE", "Fiyat negatif olamaz.");
        Glass glass = glassRepository.findById(id).orElseThrow(() -> new NotFoundException("Cam", id));
        guard.checkCatalogWrite(glass.getCompanyId(), user);
        glass.setPricePerSquareMeter(price);
        glassRepository.save(glass);
    }
}
