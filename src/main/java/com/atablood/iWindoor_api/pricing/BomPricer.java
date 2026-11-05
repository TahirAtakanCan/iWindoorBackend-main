package com.atablood.iWindoor_api.pricing;

import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.repository.AccessoryRepository;
import com.atablood.iWindoor_api.repository.GlassRepository;
import com.atablood.iWindoor_api.repository.HardwareRuleRepository;
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
 *  {"group":"HARDWARE","kind":"TILT_TURN","w":536,"h":1214,"qty":1}         donanım kurallarıyla aksesuara çevrilir
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class BomPricer {

    public static final int MAX_LINES = 2000;
    public static final String DUPLEX_ACCESSORY_CODE = "DUPLEX";
    private static final Set<String> GROUPS = Set.of("PROFILE", "BEAD", "DUPLEX", "GLASS", "PANEL", "HARDWARE");
    private static final Set<String> ROLES = Set.of("FRAME", "SASH", "MULLION", "SLIDE_SASH", "ADAPTER", "EXTENSION");
    private static final Set<String> HARDWARE_KINDS = Set.of("TURN", "TILT", "TILT_TURN", "SLIDE");

    private final ProfileRepository profileRepository;
    private final GlassRepository glassRepository;
    private final AccessoryRepository accessoryRepository;
    private final HardwareRuleRepository hardwareRuleRepository;

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
                case "HARDWARE" -> {
                    if (!(l.get("kind") instanceof String kind) || !HARDWARE_KINDS.contains(kind)) {
                        errors.add(err(p + ".kind", "Şunlardan biri olmalı: " + new TreeSet<>(HARDWARE_KINDS)));
                    }
                    if (!positive(l.get("w"), 12000)) errors.add(err(p + ".w", "0-12000 mm arası olmalı."));
                    if (!positive(l.get("h"), 12000)) errors.add(err(p + ".h", "0-12000 mm arası olmalı."));
                }
                default -> { // GLASS, PANEL
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
        Set<String> warnings = new LinkedHashSet<>();
        Resolver r = new Resolver(companyId, series, warnings);
        Map<String, Acc> acc = new LinkedHashMap<>();
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
                case "HARDWARE" -> applyHardware(r, acc, warnings, l, qty);
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

    private void applyHardware(Resolver r, Map<String, Acc> acc, Set<String> warnings, Map<String, Object> line, int lineQty) {
        String kind = (String) line.get("kind");
        double w = num(line.get("w"));
        double h = num(line.get("h"));
        List<HardwareRule> rules = r.hardwareRules(kind);
        if (rules.isEmpty()) {
            warnings.add("Donanım kuralı yok: " + kind);
            return;
        }
        for (HardwareRule rule : rules) {
            double quantity;
            if ("PER_PERIMETER_METER".equals(rule.getQtyType())) {
                quantity = rule.getQty().doubleValue() * 2.0 * (w + h) / 1000.0 * lineQty;
            } else { // PER_SASH
                quantity = rule.getQty().doubleValue() * lineQty;
            }
            Accessory a = r.accessory(rule.getAccessoryCode());
            if (a == null) {
                warnings.add("Aksesuar bulunamadı: " + rule.getAccessoryCode());
                add(acc, "ACCESSORY", rule.getAccessoryCode(), rule.getAccessoryCode(), quantity, "adet", BigDecimal.ZERO);
            } else {
                add(acc, "ACCESSORY", a.getCode(), a.getName(), quantity, unitLabel(a.getUnit()), a.getPrice());
            }
        }
    }

    private static String unitLabel(AccessoryUnit unit) {
        if (unit == null) return "adet";
        return switch (unit) {
            case PIECE -> "adet";
            case SET -> "takım";
            case METER -> "m";
        };
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
        private final Long seriesId;
        private final Set<String> warnings;
        private final List<Profile> seriesProfiles;
        private final Map<String, Optional<Glass>> glasses = new HashMap<>();
        private final Map<String, Optional<Accessory>> accessories = new HashMap<>();
        private final Map<String, List<HardwareRule>> hardwareByKind = new HashMap<>();

        Resolver(Long companyId, Series series, Set<String> warnings) {
            this.companyId = companyId;
            this.seriesId = series == null ? null : series.getId();
            this.warnings = warnings;
            this.seriesProfiles = series == null ? List.of()
                    : profileRepository.findBySeries_IdOrderByIdAsc(series.getId());
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
            Glass g = glasses.computeIfAbsent(code, c -> glassRepository.findFirstByCompanyIdAndCode(companyId, c)).orElse(null);
            if (g != null && !g.isActive()) warnings.add("Pasif kayıt: " + g.getCode());
            return g;
        }

        Accessory accessory(String code) {
            Accessory a = accessories.computeIfAbsent(code, c -> accessoryRepository.findFirstByCompanyIdAndCode(companyId, c)).orElse(null);
            if (a != null && !a.isActive()) warnings.add("Pasif kayıt: " + a.getCode());
            return a;
        }

        List<HardwareRule> hardwareRules(String kind) {
            return hardwareByKind.computeIfAbsent(kind, k -> {
                if (seriesId != null) {
                    List<HardwareRule> seriesRules = hardwareRuleRepository
                            .findByCompanyIdAndSeriesIdAndKindOrderBySortOrderAscIdAsc(companyId, seriesId, k);
                    if (!seriesRules.isEmpty()) return seriesRules;
                }
                return hardwareRuleRepository
                        .findByCompanyIdAndSeriesIdIsNullAndKindOrderBySortOrderAscIdAsc(companyId, k);
            });
        }

        /** Önce aktif varsayılan / aktif ilk; aktif yoksa pasifler arasından + uyarı. */
        private Profile pick(List<Profile> list) {
            if (list.isEmpty()) return null;
            List<Profile> active = list.stream().filter(Profile::isActive).toList();
            Profile chosen;
            if (!active.isEmpty()) {
                chosen = active.stream().filter(Profile::isDefaultForType).findFirst().orElse(active.get(0));
            } else {
                chosen = list.stream().filter(Profile::isDefaultForType).findFirst().orElse(list.get(0));
                warnings.add("Pasif kayıt: " + chosen.getCode());
            }
            return chosen;
        }
    }
}
