package com.atablood.iWindoor_api.config;

import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.ColorRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.HardwareRuleRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Şablon katalog (company_id IS NULL). Yeni şirketler kayıt olurken bunun kopyasını alır.
 * Ölçüler ve fiyatlar ÖRNEKTİR; gerçek seri kataloğundan girilmelidir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    static final String TEMPLATE_SERIES = "70 Seri (Örnek)";

    private final SeriesRepository seriesRepository;
    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;
    private final HardwareRuleRepository hardwareRuleRepository;
    private final ColorRepository colorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Her şablon kaydı ayrı kontrol edilir: eski veritabanında başka global kayıtlar olsa da örnek katalog eklenir.
        if (!seriesRepository.existsByCompanyIdIsNullAndName(TEMPLATE_SERIES)) {
            log.info("Şablon seri ve profiller ekleniyor");
            Series s = new Series(); // geometri alanları entity varsayılanlarıyla (70 seri örnek değerleri) gelir
            s.setName(TEMPLATE_SERIES);
            s.setBrand("Örnek");
            s.setSystemDepth(70);
            s.setMaterial("PVC");
            s = seriesRepository.save(s);

            profile(s, "K70-62", "Kasa 70", ProfileType.FRAME, 62, 180, null, null, true);
            profile(s, "KN70-78", "Kanat 70", ProfileType.SASH, 78, 195, null, null, true);
            profile(s, "OK70-80", "Orta Kayıt 70", ProfileType.MULLION, 80, 190, null, null, true);
            profile(s, "CT70-24", "Çıta (24 mm cam)", ProfileType.BEAD, 20, 35, 22.0, 26.0, true);
            profile(s, "CT70-32", "Çıta (32 mm cam)", ProfileType.BEAD, 20, 38, 30.0, 34.0, false);
            profile(s, "CT70-4", "Çıta (tek cam)", ProfileType.BEAD, 20, 30, 3.0, 8.0, false);
            profile(s, "AD70", "Çift Kanat Adaptörü 70", ProfileType.ADAPTER, 40, 120, null, null, true);
            profile(s, "KN70-KAPI", "Kapı Kanadı 70", ProfileType.DOOR_SASH, 110, 260, null, null, true);
            profile(s, "ESIK-AL70", "Alüminyum Eşik 70", ProfileType.THRESHOLD, 20, 310, null, null, true);
            profile(s, "BRL70", "Birleştirme Profili 70", ProfileType.COUPLING, 20, 140, null, null, true);
        } else {
            seriesRepository.findByCompanyIdIsNullOrderByIdAsc().stream()
                    .filter(s -> TEMPLATE_SERIES.equals(s.getName()))
                    .findFirst()
                    .ifPresent(s -> {
                        ensureProfile(s, "KN70-KAPI", "Kapı Kanadı 70", ProfileType.DOOR_SASH, 110, 260, true);
                        ensureProfile(s, "ESIK-AL70", "Alüminyum Eşik 70", ProfileType.THRESHOLD, 20, 310, true);
                        ensureProfile(s, "AD70", "Çift Kanat Adaptörü 70", ProfileType.ADAPTER, 40, 120, true);
                        ensureProfile(s, "BRL70", "Birleştirme Profili 70", ProfileType.COUPLING, 20, 140, true);
                    });
        }
        glass("4+16+4", "Şeffaf Isıcam 4+16+4", 24, "clear", 650);
        glass("4+16+4 LE", "Low-E Isıcam 4+16+4", 24, "lowe", 950);
        glass("4F+16+4", "Füme Isıcam 4+16+4", 24, "smoke", 900);
        glass("4B+16+4", "Bronz Isıcam 4+16+4", 24, "bronze", 900);
        glass("4R+16+4", "Reflekte Isıcam 4+16+4", 24, "mirror", 1100);
        glass("4S+16+4", "Buzlu (Satina) Isıcam 4+16+4", 24, "frost", 1000);
        glass("4D+16+4", "Desenli Isıcam 4+16+4", 24, "pattern", 850);
        glass("33.1+16+4", "Lamine Isıcam 33.1+16+4", 26, "lam", 1400);
        glass("4", "Tek Cam 4 mm", 4, "clear", 300);
        glass("PANEL-24", "Dolgu Panel 24 mm", 24, "panel", 700);
        accessory("DUPLEX", "Dubleks çıta", AccessoryUnit.METER, 45);
        accessory("KOL-STD", "Pencere kolu", AccessoryUnit.PIECE, 250);
        accessory("CONTA-EPDM", "EPDM conta", AccessoryUnit.METER, 12);
        accessory("MENTESE-SET", "Menteşe takımı", AccessoryUnit.SET, 180);
        accessory("VASISTAS-SET", "Vasistas makas takımı", AccessoryUnit.SET, 420);
        accessory("CIFT-ACILIM-SET", "Çift açılım takımı", AccessoryUnit.SET, 650);
        accessory("SURME-SET", "Sürme makara takımı", AccessoryUnit.SET, 380);
        accessory("SURME-KOL", "Sürme kolu", AccessoryUnit.PIECE, 220);
        accessory("PASIF-KANAT-SET", "Pasif kanat takımı", AccessoryUnit.SET, 320);
        accessory("KAPI-KILIT-SET", "Kapı kilit takımı", AccessoryUnit.SET, 950);
        accessory("KAPI-KOLU", "Kapı kolu", AccessoryUnit.PIECE, 380);
        accessory("KAPI-MENTESE", "Kapı menteşesi", AccessoryUnit.PIECE, 140);
        accessory("HS-SET", "Kaldır-sür takımı", AccessoryUnit.SET, 2400);
        accessory("SINEKLIK-PLISE", "Pileli sineklik", AccessoryUnit.M2, 950);
        accessory("SINEKLIK-MENTESELI", "Menteşeli sineklik", AccessoryUnit.M2, 700);
        accessory("SINEKLIK-SURME", "Sürme sineklik", AccessoryUnit.M2, 600);
        accessory("BUKUM", "Profil büküm (kemer)", AccessoryUnit.METER, 350);
        accessory("SEKILLI-CAM", "Şekilli cam farkı", AccessoryUnit.PIECE, 250);

        color("white", "Beyaz", "#F4F5F2", "#F4F5F2", 0, 10);
        color("CREAM", "Krem", "#EFE6D2", "#EFE6D2", 10, 20);
        color("ANT-1", "Antrasit (tek yüz)", "#3B3F45", "#F4F5F2", 25, 30);
        color("ANT-2", "Antrasit (çift yüz)", "#3B3F45", "#3B3F45", 40, 40);
        color("OAK-1", "Altın Meşe (tek yüz)", "#A8692F", "#F4F5F2", 30, 50);
        color("WAL-1", "Ceviz (tek yüz)", "#5C3A21", "#F4F5F2", 30, 60);

        seedHardwareRules();
    }

    private void color(String code, String name, String hexOut, String hexIn, double pct, int sort) {
        if (colorRepository.existsByCompanyIdIsNullAndCode(code)) return;
        Color c = new Color();
        c.setCode(code);
        c.setName(name);
        c.setHexOutside(hexOut);
        c.setHexInside(hexIn);
        c.setSurchargePct(BigDecimal.valueOf(pct));
        c.setSortOrder(sort);
        colorRepository.save(c);
    }

    private void seedHardwareRules() {
        rule("TURN", "KOL-STD", "PER_SASH", 1, 10);
        rule("TURN", "MENTESE-SET", "PER_SASH", 1, 20);
        rule("TURN", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("TILT", "KOL-STD", "PER_SASH", 1, 10);
        rule("TILT", "VASISTAS-SET", "PER_SASH", 1, 20);
        rule("TILT", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("TILT_TURN", "KOL-STD", "PER_SASH", 1, 10);
        rule("TILT_TURN", "CIFT-ACILIM-SET", "PER_SASH", 1, 20);
        rule("TILT_TURN", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("SLIDE", "SURME-KOL", "PER_SASH", 1, 10);
        rule("SLIDE", "SURME-SET", "PER_SASH", 1, 20);
        rule("SLIDE", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("PASSIVE", "PASIF-KANAT-SET", "PER_SASH", 1, 10);
        rule("PASSIVE", "MENTESE-SET", "PER_SASH", 1, 20);
        rule("PASSIVE", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("DOOR", "KAPI-KILIT-SET", "PER_SASH", 1, 10);
        rule("DOOR", "KAPI-KOLU", "PER_SASH", 1, 20);
        rule("DOOR", "KAPI-MENTESE", "PER_SASH", 3, 30);
        rule("DOOR", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 40);

        rule("DOOR_PASSIVE", "PASIF-KANAT-SET", "PER_SASH", 1, 10);
        rule("DOOR_PASSIVE", "KAPI-MENTESE", "PER_SASH", 3, 20);
        rule("DOOR_PASSIVE", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 30);

        rule("SLIDE_FIXED", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 10);

        rule("LIFT_SLIDE", "HS-SET", "PER_SASH", 1, 10);
        rule("LIFT_SLIDE", "CONTA-EPDM", "PER_PERIMETER_METER", 1, 20);
    }

    private void ensureProfile(Series s, String code, String name, ProfileType type, double face, double price,
                               boolean isDefault) {
        if (profileRepository.existsBySeries_IdAndType(s.getId(), type)
                || profileRepository.existsByCompanyIdIsNullAndCode(code)) {
            return;
        }
        profile(s, code, name, type, face, price, null, null, isDefault);
    }

    private void rule(String kind, String accessoryCode, String qtyType, double qty, int sortOrder) {
        if (hardwareRuleRepository.existsByCompanyIdIsNullAndKindAndAccessoryCodeAndSeriesIdIsNull(kind, accessoryCode)) {
            return;
        }
        HardwareRule r = new HardwareRule();
        r.setKind(kind);
        r.setAccessoryCode(accessoryCode);
        r.setQtyType(qtyType);
        r.setQty(BigDecimal.valueOf(qty));
        r.setSortOrder(sortOrder);
        hardwareRuleRepository.save(r);
    }

    private void profile(Series s, String code, String name, ProfileType type, double face, double price,
                         Double minGlass, Double maxGlass, boolean isDefault) {
        Profile p = new Profile();
        p.setSeries(s);
        p.setCode(code);
        p.setName(name);
        p.setType(type);
        p.setWidthMm(face);
        p.setPricePerMeter(BigDecimal.valueOf(price));
        p.setMinGlassMm(minGlass);
        p.setMaxGlassMm(maxGlass);
        p.setDefaultForType(isDefault);
        profileRepository.save(p);
    }

    private void glass(String code, String name, int thickness, String style, double price) {
        if (glassRepository.existsByCompanyIdIsNullAndCode(code)) return;
        Glass g = new Glass();
        g.setCode(code);
        g.setName(name);
        g.setThicknessMm(thickness);
        g.setVisualStyle(style);
        g.setPricePerSquareMeter(BigDecimal.valueOf(price));
        glassRepository.save(g);
    }

    private void accessory(String code, String name, AccessoryUnit unit, double price) {
        if (accessoryRepository.existsByCompanyIdIsNullAndCode(code)) return;
        Accessory a = new Accessory();
        a.setCode(code);
        a.setName(name);
        a.setUnit(unit);
        a.setPrice(BigDecimal.valueOf(price));
        accessoryRepository.save(a);
    }
}
