package com.atablood.iWindoor_api.pricing;

import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BomPricerTest {

    private static final long COMPANY = 7L;
    private ProfileRepository profiles;
    private GlassRepository glasses;
    private AccessoryRepository accessories;
    private BomPricer pricer;
    private Series series;

    private static Profile profile(long id, ProfileType type, String code, double price, Double min, Double max, boolean def) {
        Profile p = new Profile();
        p.setId(id);
        p.setType(type);
        p.setCode(code);
        p.setName(code);
        p.setPricePerMeter(BigDecimal.valueOf(price));
        p.setMinGlassMm(min);
        p.setMaxGlassMm(max);
        p.setDefaultForType(def);
        return p;
    }

    @BeforeEach
    void setUp() {
        profiles = mock(ProfileRepository.class);
        glasses = mock(GlassRepository.class);
        accessories = mock(AccessoryRepository.class);
        pricer = new BomPricer(profiles, glasses, accessories);
        series = new Series();
        series.setId(1L);
        when(profiles.findBySeries_IdOrderByIdAsc(1L)).thenReturn(List.of(
                profile(1, ProfileType.FRAME, "K70-62", 180, null, null, true),
                profile(2, ProfileType.SASH, "KN70-78", 195, null, null, true),
                profile(3, ProfileType.MULLION, "OK70-80", 190, null, null, true),
                profile(4, ProfileType.BEAD, "CT70-4", 30, 3.0, 8.0, false),
                profile(5, ProfileType.BEAD, "CT70-24", 35, 22.0, 26.0, true)));
        Glass g = new Glass();
        g.setCode("4+16+4");
        g.setName("Şeffaf");
        g.setThicknessMm(24);
        g.setPricePerSquareMeter(BigDecimal.valueOf(650));
        when(glasses.findFirstByCompanyIdAndCode(COMPANY, "4+16+4")).thenReturn(Optional.of(g));
        Glass single = new Glass();
        single.setCode("4");
        single.setName("Tek cam");
        single.setThicknessMm(4);
        single.setPricePerSquareMeter(BigDecimal.valueOf(300));
        when(glasses.findFirstByCompanyIdAndCode(COMPANY, "4")).thenReturn(Optional.of(single));
        when(glasses.findFirstByCompanyIdAndCode(eq(COMPANY), argThat(c -> !"4+16+4".equals(c) && !"4".equals(c))))
                .thenReturn(Optional.empty());
        when(accessories.findFirstByCompanyIdAndCode(anyLong(), anyString())).thenReturn(Optional.empty());
    }

    private static Map<String, Object> bar(String group, String role, double len, int qty) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("group", group);
        if (role != null) m.put("role", role);
        m.put("length", len);
        m.put("qty", qty);
        m.put("glassCode", "4+16+4");
        return m;
    }

    private static Map<String, Object> sheet(String code, double w, double h, int qty) {
        return new LinkedHashMap<>(Map.of("group", "GLASS", "glassCode", code, "w", w, "h", h, "qty", qty));
    }

    /** Prototipteki 1800x1700 pencerenin kesim listesi; beklenen 4816,31 TL elle hesaplandı. */
    @Test
    void pricesSampleWindowExactly() {
        List<Map<String, Object>> bom = List.of(
                bar("PROFILE", "FRAME", 1806, 2), bar("PROFILE", "FRAME", 1706, 2),
                bar("PROFILE", "MULLION", 1682, 1), bar("PROFILE", "MULLION", 1204, 2), bar("PROFILE", "MULLION", 304, 2),
                bar("PROFILE", "SASH", 542, 2), bar("PROFILE", "SASH", 1220, 2),
                bar("BEAD", null, 1198, 4), bar("BEAD", null, 1058, 2), bar("BEAD", null, 520, 2),
                bar("BEAD", null, 498, 8), bar("BEAD", null, 380, 2), bar("BEAD", null, 298, 6),
                sheet("4+16+4", 548, 326, 1), sheet("4+16+4", 526, 326, 2), sheet("4+16+4", 526, 1226, 2), sheet("4+16+4", 408, 1086, 1));

        PricedBom p = pricer.price(COMPANY, series, bom);

        assertThat(p.warnings()).isEmpty();
        assertThat(p.total()).isEqualByComparingTo("4816.31");
        Map<String, BigDecimal> byCode = new HashMap<>();
        for (QuoteLine l : p.lines()) byCode.put(l.code(), l.total());
        assertThat(byCode.get("K70-62")).isEqualByComparingTo("1264.32");   // 7,024 m x 180
        assertThat(byCode.get("OK70-80")).isEqualByComparingTo("892.62");   // 4,698 m x 190
        assertThat(byCode.get("KN70-78")).isEqualByComparingTo("687.18");   // 3,524 m x 195
        assertThat(byCode.get("CT70-24")).isEqualByComparingTo("506.80");   // 14,48 m x 35 (24 mm cam -> 22-26 aralığı)
        assertThat(byCode.get("4+16+4")).isEqualByComparingTo("1465.39");   // 2,25444 m2 x 650
    }

    @Test
    void beadFollowsGlassThickness() {
        Map<String, Object> l = bar("BEAD", null, 1000, 1);
        l.put("glassCode", "4");
        PricedBom p = pricer.price(COMPANY, series, List.of(l));
        assertThat(p.lines().get(0).code()).isEqualTo("CT70-4");
    }

    @Test
    void unknownMaterialsArePricedZeroWithWarning() {
        PricedBom p = pricer.price(COMPANY, series, List.of(
                sheet("YOK-123", 1000, 1000, 1),
                bar("PROFILE", "EXTENSION", 1000, 1),
                bar("DUPLEX", null, 1000, 1)));
        assertThat(p.total()).isEqualByComparingTo("0");
        assertThat(p.warnings()).hasSize(3);
    }

    @Test
    void rejectsMalformedLines() {
        Map<String, Object> bad = new LinkedHashMap<>(Map.of("group", "PROFILE", "role", "WINDOWSILL", "length", -1, "qty", 0));
        assertThatThrownBy(() -> pricer.price(COMPANY, series, List.of(bad)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((BadRequestException) e).getCode()).isEqualTo("INVALID_BOM"));
    }
}
