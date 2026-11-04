package com.atablood.iWindoor_api.pricing;

import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * İstemcinin çizim motorundan gelen kesim listesini (BOM) katalog fiyatlarıyla çarpar.
 * Miktarın kaynağı motor, fiyatın kaynağı sunucu.
 *
 * <pre>
 * Satır biçimi (1 pencere için):
 *  {"group":"PROFILE","role":"FRAME","length":1806,"qty":2,"cut":"45/45"}   role: FRAME|SASH|MULLION|SLIDE_SASH|ADAPTER|EXTENSION
 *  {"group":"BEAD","length":1198,"qty":4,"glassCode":"4+16+4"}               çıta, camın kalınlığına göre seçilir
 *  {"group":"DUPLEX","length":458,"qty":1}                                  aksesuar kodu DUPLEX (metre)
 *  {"group":"GLASS","glassCode":"4+16+4","w":408,"h":1086,"qty":1}
 *  {"group":"PANEL","glassCode":"PANEL-24","w":408,"h":1086,"qty":1}
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class BomPricer {

    public static final int MAX_LINES = 2000;
    public static final String DUPLEX_ACCESSORY_CODE = "DUPLEX";
    private static final Set<String> GROUPS = Set.of("PROFILE", "BEAD", "DUPLEX", "GLASS", "PANEL");
    private static final Set<String> ROLES = Set.of("FRAME", "SASH", "MULLION", "SLIDE_SASH", "ADAPTER", "EXTENSION");

    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;

    /** Satırların biçimini kontrol eder; hatalıysa BadRequestException (code=INVALID_BOM). */
    public void validate(List<Map<String, Object>> bom) {
        if (bom == null) return;
        List<Map<String, String>> errors = new ArrayList<>();
        if (bom.size() > MAX_LINES) errors.add(err("$", "En fazla " + MAX_LINES + " satır."));
        for (int i = 0; i < bom.size() && errors.size() < 50; i++) {
            Map<String, Object> l = bom.get(i);
            String p = "$[" + i + "]";
            if (l == null) {
                errors.add(err(p, "Boş satır."));
                continue;
            }
            Object g = l.get("group");
            if (!(g instanceof String group) || !GROUPS.contains(group)) {
                errors.add(err(p + ".group", "Şunlardan biri olmalı: " + new TreeSet<>(GROUPS)));
                continue;
            }
            Object qty = l.get("qty");
            if (!(qty instanceof Number q) || q.doubleValue() != Math.floor(q.doubleValue()) || q.intValue() < 1 || q.intValue() > 10000) {
                errors.add(err(p + ".qty", "1-10000 arası tam sayı olmalı."));
            }
            switch (group) {
                case "PROFILE", "BEAD", "DUPLEX" -> {
                    if (!positive(l.get("length"), 20000)) errors.add(err(p + ".length", "0-20000 mm arası olmalı."));
                    if ("PROFILE".equals(group) && (!(l.get("role") instanceof String r) || !ROLES.contains(r))) {
                        errors.add(err(p + ".role", "Şunlardan biri olmalı: " + new TreeSet<>(ROLES)));
                    }
                }
                default -> {
                    if (!positive(l.get("w"), 12000)) errors.add(err(p + ".w", "0-12000 mm arası olmalı."));
                    if (!positive(l.get("h"), 12000)) errors.add(err(p + ".h", "0-12000 mm arası olmalı."));
                    if (!(l.get("glassCode") instanceof String gc) || gc.isBlank()) {
                        errors.add(err(p + ".glassCode", "Cam/panel kodu gerekli."));
                    }
                }
            }
        }
        if (!errors.isEmpty()) throw new BadRequestException("INVALID_BOM", "Kesim listesi geçersiz.", errors);
    }

    /** Bir adet pencerenin kesim listesini fiyatlar. Bulunamayan malzemeler 0 fiyatla ve uyarıyla döner. */
    public PricedBom price(Long companyId, Series series, List<Map<String, Object>> bom) {
        validate(bom);
        Resolver r = new Resolver(companyId, series);
        Map<String, Acc> acc = new LinkedHashMap<>();
        Set<String> warnings = new LinkedHashSet<>();
        String currency = "TRY";

        for (Map<String, Object> l : bom == null ? List.<Map<String, Object>>of() : bom) {
            String group = (String) l.get("group");
            int qty = ((Number) l.get("qty")).intValue();
            switch (group) {
                case "PROFILE" -> {
                    String role = (String) l.get("role");
                    ProfileType type = "SLIDE_SASH".equals(role) ? ProfileType.SASH : ProfileType.valueOf(role);
                    Profile pr = r.profile(type);
                    double m = num(l.get("length")) / 1000.0 * qty;
                    if (pr == null) warnings.add("Seride " + type + " profili yok; " + role + " fiyatlanmadı.");
                    add(acc, group, pr == null ? role : pr.getCode(), pr == null ? role : pr.getName(), m, "m",
                            pr == null ? BigDecimal.ZERO : pr.getPricePerMeter());
                    if (pr != null && pr.getCurrency() != null) currency = pr.getCurrency();
                }
                case "BEAD" -> {
                    Glass g = r.glass((String) l.get("glassCode"));
                    Profile bead = r.bead(g == null ? null : g.getThicknessMm());
                    double m = num(l.get("length")) / 1000.0 * qty;
                    if (bead == null) warnings.add("Seride çıta (BEAD) profili yok.");
                    add(acc, group, bead == null ? "BEAD" : bead.getCode(), bead == null ? "Cam çıtası" : bead.getName(), m, "m",
                            bead == null ? BigDecimal.ZERO : bead.getPricePerMeter());
                }
                case "DUPLEX" -> {
                    Accessory a = r.accessory(DUPLEX_ACCESSORY_CODE);
                    double m = num(l.get("length")) / 1000.0 * qty;
                    if (a == null) warnings.add("Aksesuar kodu " + DUPLEX_ACCESSORY_CODE + " yok; dubleks fiyatlanmadı.");
                    add(acc, group, DUPLEX_ACCESSORY_CODE, a == null ? "Dubleks çıta" : a.getName(), m, "m",
                            a == null ? BigDecimal.ZERO : a.getPrice());
                }
                default -> { // GLASS, PANEL
                    String code = (String) l.get("glassCode");
                    Glass g = r.glass(code);
                    double m2 = num(l.get("w")) * num(l.get("h")) / 1_000_000.0 * qty;
                    if (g == null) warnings.add("Katalogda cam/panel kodu yok: " + code);
                    add(acc, group, code, g == null ? code : g.getName(), m2, "m2",
                            g == null ? BigDecimal.ZERO : g.getPricePerSquareMeter());
                }
            }
        }

        List<QuoteLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Acc a : acc.values()) {
            BigDecimal lineTotal = a.unitPrice.multiply(BigDecimal.valueOf(a.quantity)).setScale(2, RoundingMode.HALF_UP);
            lines.add(new QuoteLine(a.group, a.code, a.name, round(a.quantity, 3), a.unit, a.unitPrice, lineTotal));
            total = total.add(lineTotal);
        }
        return new PricedBom(lines, total.setScale(2, RoundingMode.HALF_UP), currency, new ArrayList<>(warnings));
    }

    private static void add(Map<String, Acc> acc, String group, String code, String name, double q, String unit, BigDecimal unitPrice) {
        acc.computeIfAbsent(group + "|" + code, k -> new Acc(group, code, name, unit, unitPrice)).quantity += q;
    }

    private static boolean positive(Object v, double max) {
        return v instanceof Number n && n.doubleValue() > 0 && n.doubleValue() <= max;
    }

    private static double num(Object v) {
        return ((Number) v).doubleValue();
    }

    private static double round(double v, int digits) {
        double f = Math.pow(10, digits);
        return Math.round(v * f) / f;
    }

    private static Map<String, String> err(String path, String message) {
        Map<String, String> e = new LinkedHashMap<>();
        e.put("path", path);
        e.put("message", message);
        return e;
    }

    private static final class Acc {
        final String group, code, name, unit;
        final BigDecimal unitPrice;
        double quantity;

        Acc(String group, String code, String name, String unit, BigDecimal unitPrice) {
            this.group = group;
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        }
    }

    /** Katalog sorgularını bir fiyatlama boyunca önbellekler. */
    private final class Resolver {
        private final Long companyId;
        private final List<Profile> seriesProfiles;
        private final Map<String, Optional<Glass>> glasses = new HashMap<>();
        private final Map<String, Optional<Accessory>> accessories = new HashMap<>();

        Resolver(Long companyId, Series series) {
            this.companyId = companyId;
            this.seriesProfiles = series == null ? List.of()
                    : profileRepository.findBySeries_IdOrderByIdAsc(series.getId()).stream().filter(Profile::isActive).toList();
        }

        Profile profile(ProfileType type) {
            return pick(seriesProfiles.stream().filter(p -> p.getType() == type).toList());
        }

        Profile bead(Integer glassThickness) {
            List<Profile> beads = seriesProfiles.stream().filter(p -> p.getType() == ProfileType.BEAD).toList();
            if (glassThickness != null) {
                List<Profile> fit = beads.stream().filter(b -> b.getMinGlassMm() != null && b.getMaxGlassMm() != null
                        && glassThickness >= b.getMinGlassMm() && glassThickness <= b.getMaxGlassMm()).toList();
                if (!fit.isEmpty()) return pick(fit);
            }
            return pick(beads);
        }

        Glass glass(String code) {
            if (code == null) return null;
            return glasses.computeIfAbsent(code, c -> glassRepository.findFirstByCompanyIdAndCode(companyId, c)).orElse(null);
        }

        Accessory accessory(String code) {
            return accessories.computeIfAbsent(code, c -> accessoryRepository.findFirstByCompanyIdAndCode(companyId, c)).orElse(null);
        }

        private Profile pick(List<Profile> list) {
            return list.stream().filter(Profile::isDefaultForType).findFirst().orElse(list.isEmpty() ? null : list.get(0));
        }
    }
}
