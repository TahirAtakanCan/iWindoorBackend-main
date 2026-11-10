package com.atablood.iWindoor_api.pricing;

import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.ColorRepository;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.HardwareRuleRepository;
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
    private HardwareRuleRepository hardwareRules;
    private ColorRepository colors;
    private CompanyRepository companies;
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

    private static Accessory accessory(String code, String name, AccessoryUnit unit, double price) {
        Accessory a = new Accessory();
        a.setCode(code);
        a.setName(name);
        a.setUnit(unit);
        a.setPrice(BigDecimal.valueOf(price));
        return a;
    }

    private static HardwareRule rule(String kind, String code, String qtyType, double qty, int sort) {
        HardwareRule r = new HardwareRule();
        r.setKind(kind);
        r.setAccessoryCode(code);
        r.setQtyType(qtyType);
        r.setQty(BigDecimal.valueOf(qty));
        r.setSortOrder(sort);
        return r;
    }

    @BeforeEach
    void setUp() {
        profiles = mock(ProfileRepository.class);
        glasses = mock(GlassRepository.class);
        accessories = mock(AccessoryRepository.class);
        hardwareRules = mock(HardwareRuleRepository.class);
        colors = mock(ColorRepository.class);
        companies = mock(CompanyRepository.class);
        pricer = new BomPricer(profiles, glasses, accessories, hardwareRules, colors, companies);
        series = new Series();
        series.setId(1L);
        Company company = new Company();
        company.setId(COMPANY);
        company.setScreenMinAreaM2(new BigDecimal("0.50"));
        when(companies.findById(COMPANY)).thenReturn(Optional.of(company));
        when(colors.findFirstByCompanyIdAndCode(anyLong(), anyString())).thenReturn(Optional.empty());
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
        when(hardwareRules.findByCompanyIdAndSeriesIdAndKindOrderBySortOrderAscIdAsc(anyLong(), anyLong(), anyString()))
                .thenReturn(List.of());
        when(hardwareRules.findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(anyLong(), anyString()))
                .thenReturn(List.of());
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

    private static List<Map<String, Object>> sampleBom() {
        return List.of(
                bar("PROFILE", "FRAME", 1806, 2), bar("PROFILE", "FRAME", 1706, 2),
                bar("PROFILE", "MULLION", 1682, 1), bar("PROFILE", "MULLION", 1204, 2), bar("PROFILE", "MULLION", 304, 2),
                bar("PROFILE", "SASH", 542, 2), bar("PROFILE", "SASH", 1220, 2),
                bar("BEAD", null, 1198, 4), bar("BEAD", null, 1058, 2), bar("BEAD", null, 520, 2),
                bar("BEAD", null, 498, 8), bar("BEAD", null, 380, 2), bar("BEAD", null, 298, 6),
                sheet("4+16+4", 548, 326, 1), sheet("4+16+4", 526, 326, 2), sheet("4+16+4", 526, 1226, 2), sheet("4+16+4", 408, 1086, 1));
    }

    /** Prototipteki 1800x1700 pencerenin kesim listesi; beklenen 4816,31 TL elle hesaplandı. */
    @Test
    void pricesSampleWindowExactly() {
        PricedBom p = pricer.price(COMPANY, series, sampleBom());

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
    void tiltTurnHardwareAddsAccessories() {
        when(hardwareRules.findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(COMPANY, "TILT_TURN"))
                .thenReturn(List.of(
                        rule("TILT_TURN", "KOL-STD", "PER_SASH", 1, 10),
                        rule("TILT_TURN", "CIFT-ACILIM-SET", "PER_SASH", 1, 20),
                        rule("TILT_TURN", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "KOL-STD"))
                .thenReturn(Optional.of(accessory("KOL-STD", "Pencere kolu", AccessoryUnit.PIECE, 250)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "CIFT-ACILIM-SET"))
                .thenReturn(Optional.of(accessory("CIFT-ACILIM-SET", "Çift açılım takımı", AccessoryUnit.SET, 650)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "CONTA-EPDM"))
                .thenReturn(Optional.of(accessory("CONTA-EPDM", "EPDM conta", AccessoryUnit.METER, 12)));

        Map<String, Object> hw = new LinkedHashMap<>();
        hw.put("group", "HARDWARE");
        hw.put("kind", "TILT_TURN");
        hw.put("w", 536);
        hw.put("h", 1214);
        hw.put("qty", 1);

        PricedBom onlyHw = pricer.price(COMPANY, series, List.of(hw));
        Map<String, QuoteLine> byCode = new HashMap<>();
        for (QuoteLine l : onlyHw.lines()) byCode.put(l.code(), l);
        assertThat(byCode.get("KOL-STD").quantity()).isEqualTo(1.0);
        assertThat(byCode.get("KOL-STD").unit()).isEqualTo("adet");
        assertThat(byCode.get("KOL-STD").total()).isEqualByComparingTo("250.00");
        assertThat(byCode.get("CIFT-ACILIM-SET").unit()).isEqualTo("takım");
        assertThat(byCode.get("CIFT-ACILIM-SET").total()).isEqualByComparingTo("650.00");
        assertThat(byCode.get("CONTA-EPDM").quantity()).isEqualTo(3.5);
        assertThat(byCode.get("CONTA-EPDM").unit()).isEqualTo("m");
        assertThat(byCode.get("CONTA-EPDM").total()).isEqualByComparingTo("42.00");
        assertThat(onlyHw.total()).isEqualByComparingTo("942.00");

        List<Map<String, Object>> withHw = new ArrayList<>(sampleBom());
        withHw.add(hw);
        PricedBom full = pricer.price(COMPANY, series, withHw);
        assertThat(full.total()).isEqualByComparingTo("5758.31");
    }

    @Test
    void bomWithoutHardwareKeepsOldTotal() {
        PricedBom p = pricer.price(COMPANY, series, sampleBom());
        assertThat(p.total()).isEqualByComparingTo("4816.31");
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

    @Test
    void adapterUsesAd70Price() {
        when(profiles.findBySeries_IdOrderByIdAsc(1L)).thenReturn(List.of(
                profile(1, ProfileType.FRAME, "K70-62", 180, null, null, true),
                profile(2, ProfileType.SASH, "KN70-78", 195, null, null, true),
                profile(6, ProfileType.ADAPTER, "AD70", 120, null, null, true)));
        Map<String, Object> line = bar("PROFILE", "ADAPTER", 1700, 1);
        line.put("cut", "90/90");
        PricedBom p = pricer.price(COMPANY, series, List.of(line));
        assertThat(p.lines().get(0).code()).isEqualTo("AD70");
        assertThat(p.lines().get(0).total()).isEqualByComparingTo("204.00"); // 1.7 m × 120
        assertThat(p.warnings()).isEmpty();
    }

    @Test
    void doorSashFallsBackToSashWithWarning() {
        Map<String, Object> line = bar("PROFILE", "DOOR_SASH", 2000, 1);
        PricedBom p = pricer.price(COMPANY, series, List.of(line));
        assertThat(p.lines().get(0).code()).isEqualTo("KN70-78");
        assertThat(p.lines().get(0).total()).isEqualByComparingTo("390.00"); // 2 m × 195
        assertThat(p.warnings()).contains("Seride kapı kanadı profili yok; kanat profiliyle fiyatlandı.");
    }

    @Test
    void doorHardwareQuantities() {
        when(hardwareRules.findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(COMPANY, "DOOR"))
                .thenReturn(List.of(
                        rule("DOOR", "KAPI-KILIT-SET", "PER_SASH", 1, 10),
                        rule("DOOR", "KAPI-KOLU", "PER_SASH", 1, 20),
                        rule("DOOR", "KAPI-MENTESE", "PER_SASH", 3, 30),
                        rule("DOOR", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 40)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "KAPI-KILIT-SET"))
                .thenReturn(Optional.of(accessory("KAPI-KILIT-SET", "Kapı kilit takımı", AccessoryUnit.SET, 950)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "KAPI-KOLU"))
                .thenReturn(Optional.of(accessory("KAPI-KOLU", "Kapı kolu", AccessoryUnit.PIECE, 380)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "KAPI-MENTESE"))
                .thenReturn(Optional.of(accessory("KAPI-MENTESE", "Kapı menteşesi", AccessoryUnit.PIECE, 140)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "CONTA-EPDM"))
                .thenReturn(Optional.of(accessory("CONTA-EPDM", "EPDM conta", AccessoryUnit.METER, 12)));

        Map<String, Object> hw = new LinkedHashMap<>();
        hw.put("group", "HARDWARE");
        hw.put("kind", "DOOR");
        hw.put("w", 792);
        hw.put("h", 2034);
        hw.put("qty", 1);

        PricedBom p = pricer.price(COMPANY, series, List.of(hw));
        Map<String, QuoteLine> byCode = new HashMap<>();
        for (QuoteLine l : p.lines()) byCode.put(l.code(), l);
        assertThat(byCode.get("KAPI-KILIT-SET").quantity()).isEqualTo(1.0);
        assertThat(byCode.get("KAPI-KOLU").quantity()).isEqualTo(1.0);
        assertThat(byCode.get("KAPI-MENTESE").quantity()).isEqualTo(3.0);
        assertThat(byCode.get("KAPI-MENTESE").total()).isEqualByComparingTo("420.00");
        assertThat(byCode.get("CONTA-EPDM").quantity()).isEqualTo(5.652);
        assertThat(byCode.get("CONTA-EPDM").unit()).isEqualTo("m");
    }

    @Test
    void colorSurchargeOnProfileAndBead() {
        Color ant = new Color();
        ant.setCode("ANT-1");
        ant.setName("Antrasit (tek yüz)");
        ant.setSurchargePct(BigDecimal.valueOf(25));
        ant.setActive(true);
        when(colors.findFirstByCompanyIdAndCode(COMPANY, "ANT-1")).thenReturn(Optional.of(ant));

        List<Map<String, Object>> bom = new ArrayList<>(sampleBom());
        bom.add(new LinkedHashMap<>(Map.of("group", "COLOR", "colorCode", "ANT-1", "qty", 1)));

        PricedBom without = pricer.price(COMPANY, series, sampleBom());
        PricedBom with = pricer.price(COMPANY, series, bom);

        BigDecimal profileBead = BigDecimal.ZERO;
        for (QuoteLine l : without.lines()) {
            if ("PROFILE".equals(l.group()) || "BEAD".equals(l.group())) profileBead = profileBead.add(l.total());
        }
        BigDecimal fark = profileBead.multiply(BigDecimal.valueOf(25))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        QuoteLine colorLine = with.lines().stream().filter(l -> "COLOR".equals(l.group())).findFirst().orElseThrow();
        assertThat(colorLine.code()).isEqualTo("ANT-1");
        assertThat(colorLine.unit()).isEqualTo("—");
        assertThat(colorLine.total()).isEqualByComparingTo(fark);
        assertThat(with.total()).isEqualByComparingTo(without.total().add(fark));
        assertThat(pricer.price(COMPANY, series, sampleBom()).total()).isEqualByComparingTo(without.total());
    }

    @Test
    void unknownColorIsZeroWithWarning() {
        List<Map<String, Object>> bom = new ArrayList<>(sampleBom());
        bom.add(new LinkedHashMap<>(Map.of("group", "COLOR", "colorCode", "YOK", "qty", 1)));
        PricedBom p = pricer.price(COMPANY, series, bom);
        assertThat(p.warnings()).contains("Renk bulunamadı: YOK");
        QuoteLine colorLine = p.lines().stream().filter(l -> "COLOR".equals(l.group())).findFirst().orElseThrow();
        assertThat(colorLine.total()).isEqualByComparingTo("0");
    }

    @Test
    void screenPliseUsesActualArea() {
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "SINEKLIK-PLISE"))
                .thenReturn(Optional.of(accessory("SINEKLIK-PLISE", "Pileli sineklik", AccessoryUnit.M2, 950)));
        Map<String, Object> screen = new LinkedHashMap<>(Map.of(
                "group", "SCREEN", "kind", "PLISE", "w", 576, "h", 1176, "qty", 1));
        PricedBom p = pricer.price(COMPANY, series, List.of(screen));
        QuoteLine line = p.lines().get(0);
        assertThat(line.group()).isEqualTo("ACCESSORY");
        assertThat(line.code()).isEqualTo("SINEKLIK-PLISE");
        assertThat(line.unit()).isEqualTo("m²");
        assertThat(line.quantity()).isEqualTo(0.677);
        assertThat(line.total()).isEqualByComparingTo("643.51");
    }

    @Test
    void screenSlidingAppliesMinArea() {
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "SINEKLIK-SURME"))
                .thenReturn(Optional.of(accessory("SINEKLIK-SURME", "Sürme sineklik", AccessoryUnit.M2, 600)));
        Map<String, Object> screen = new LinkedHashMap<>(Map.of(
                "group", "SCREEN", "kind", "SLIDING", "w", 400, "h", 500, "qty", 1));
        PricedBom p = pricer.price(COMPANY, series, List.of(screen));
        QuoteLine line = p.lines().get(0);
        assertThat(line.code()).isEqualTo("SINEKLIK-SURME");
        assertThat(line.quantity()).isEqualTo(0.5);
        assertThat(line.total()).isEqualByComparingTo("300.00");
    }

    @Test
    void liftSlideAndSlideFixedHardware() {
        when(hardwareRules.findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(COMPANY, "LIFT_SLIDE"))
                .thenReturn(List.of(
                        rule("LIFT_SLIDE", "HS-SET", "PER_SASH", 1, 10),
                        rule("LIFT_SLIDE", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 20)));
        when(hardwareRules.findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(COMPANY, "SLIDE_FIXED"))
                .thenReturn(List.of(rule("SLIDE_FIXED", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 10)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "HS-SET"))
                .thenReturn(Optional.of(accessory("HS-SET", "Kaldır-sür takımı", AccessoryUnit.SET, 2400)));
        when(accessories.findFirstByCompanyIdAndCode(COMPANY, "CONTA-EPDM"))
                .thenReturn(Optional.of(accessory("CONTA-EPDM", "EPDM conta", AccessoryUnit.METER, 12)));

        Map<String, Object> lift = new LinkedHashMap<>(Map.of(
                "group", "HARDWARE", "kind", "LIFT_SLIDE", "w", 1000, "h", 2000, "qty", 1));
        PricedBom liftBom = pricer.price(COMPANY, series, List.of(lift));
        Map<String, QuoteLine> liftBy = new HashMap<>();
        for (QuoteLine l : liftBom.lines()) liftBy.put(l.code(), l);
        assertThat(liftBy.keySet()).containsExactlyInAnyOrder("HS-SET", "CONTA-EPDM");
        assertThat(liftBy.get("HS-SET").quantity()).isEqualTo(1.0);
        assertThat(liftBy.get("HS-SET").unit()).isEqualTo("takım");
        assertThat(liftBy.get("CONTA-EPDM").quantity()).isEqualTo(6.0);

        Map<String, Object> fixed = new LinkedHashMap<>(Map.of(
                "group", "HARDWARE", "kind", "SLIDE_FIXED", "w", 1000, "h", 2000, "qty", 1));
        PricedBom fixedBom = pricer.price(COMPANY, series, List.of(fixed));
        assertThat(fixedBom.lines()).hasSize(1);
        assertThat(fixedBom.lines().get(0).code()).isEqualTo("CONTA-EPDM");
        assertThat(fixedBom.lines().get(0).quantity()).isEqualTo(6.0);
    }
}
