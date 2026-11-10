package com.atablood.iWindoor_api.design;

import com.atablood.iWindoor_api.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tasarım belgesinin (Dart: WindowDesign.toJson) yapısal kontrolü.
 * Geometri hesabı istemcide yapılır; sunucu sadece belgenin bozuk olmadığından emin olur.
 */
@Component
public class DesignDocumentValidator {

    public static final int SCHEMA_VERSION = 1;
    public static final Set<Integer> SUPPORTED_SCHEMA_VERSIONS = Set.of(1, 2);
    public static final double MIN_SIZE_MM = 200, MAX_WIDTH_MM = 12000, MAX_HEIGHT_MM = 6000;
    public static final int MAX_DEPTH = 16, MAX_NODES = 400, MAX_SPLIT_PARTS = 12, MAX_DUPLEX = 12;
    public static final int MAX_COUPLE_FRAMES = 6;
    public static final Set<String> OPENINGS = Set.of("turnLeft", "turnRight", "tilt", "tiltTurnLeft", "tiltTurnRight");
    public static final Set<String> DOOR_OPENINGS = Set.of("turnLeft", "turnRight");
    public static final Set<String> SCREENS = Set.of("plise", "hinged", "sliding");
    public static final Set<String> SLIDE_MOVES = Set.of("left", "right", "fixed");
    public static final Set<String> SLIDE_SYSTEMS = Set.of("standard", "liftSlide");
    public static final Set<String> SHAPE_TYPES = Set.of("rect", "arch", "segmentArch", "trapezoid", "triangle");
    public static final Set<String> TRIANGLE_APEX = Set.of("center", "left", "right");

    public void validateOrThrow(Map<String, Object> doc) {
        List<Map<String, String>> errors = validate(doc);
        if (!errors.isEmpty()) {
            throw new BadRequestException("INVALID_DESIGN", "Tasarım belgesi geçersiz.", errors);
        }
    }

    public List<Map<String, String>> validate(Map<String, Object> doc) {
        Ctx c = new Ctx();
        if (doc == null) {
            c.err("$", "Belge boş.");
            return c.errors;
        }
        Object v = doc.get("schemaVersion");
        if (!(v instanceof Number n) || !SUPPORTED_SCHEMA_VERSIONS.contains(n.intValue())) {
            c.err("$.schemaVersion", "Desteklenen sürüm: 1 veya 2");
        } else {
            c.schemaVersion = n.intValue();
        }
        number(c, doc, "width", "$", MIN_SIZE_MM, MAX_WIDTH_MM);
        number(c, doc, "height", "$", MIN_SIZE_MM, MAX_HEIGHT_MM);
        Double designW = asPositiveNumber(doc.get("width"));
        Double designH = asPositiveNumber(doc.get("height"));
        Object series = doc.get("series");
        if (series != null && !(series instanceof String) && !(series instanceof Number)) {
            c.err("$.series", "Metin veya sayı olmalı.");
        }
        Object color = doc.get("color");
        if (color != null && !(color instanceof String)) c.err("$.color", "Metin olmalı.");
        Object bottom = doc.get("bottom");
        if (bottom != null && !"frame".equals(bottom) && !"threshold".equals(bottom)) {
            c.err("$.bottom", "frame veya threshold olmalı.");
        }
        Object root = doc.get("root");
        if (!(root instanceof Map<?, ?> rm)) {
            c.err("$.root", "Kök düğüm eksik.");
        } else {
            if ("couple".equals(rm.get("type")) && bottom != null) {
                c.err("$.bottom", "Birleşik doğrama kökünde bottom kullanılamaz.");
            }
            if (rm.containsKey("shape")) {
                if ("couple".equals(rm.get("type"))) {
                    c.err("$.root.shape", "Birleşik doğrama kökünde shape kullanılamaz.");
                } else {
                    requireSchema2ForShape(c);
                    validateShape(c, rm, "$.root", designW, designH);
                }
            }
            node(c, root, "$.root", 1, null, null, designW, designH);
        }
        return c.errors;
    }

    private void node(Ctx c, Object raw, String path, int depth, String parentType, String coupleDir,
                      Double designW, Double designH) {
        if (c.errors.size() > 50) return;
        if (!(raw instanceof Map<?, ?> m)) {
            c.err(path, "Düğüm nesne olmalı.");
            return;
        }
        if (depth > MAX_DEPTH) {
            c.err(path, "Ağaç çok derin (en fazla " + MAX_DEPTH + ").");
            return;
        }
        if (++c.nodes > MAX_NODES) {
            if (c.nodes == MAX_NODES + 1) c.err(path, "Çok fazla düğüm (en fazla " + MAX_NODES + ").");
            return;
        }
        Object id = m.get("id");
        if (!(id instanceof String s) || s.isBlank() || s.length() > 64) {
            c.err(path + ".id", "1-64 karakterlik metin olmalı.");
        } else if (!c.ids.add(s)) {
            c.err(path + ".id", "Tekrarlanan id: " + s);
        }
        Object type = m.get("type");
        if (!(type instanceof String t)) {
            c.err(path + ".type", "Eksik.");
            return;
        }
        switch (t) {
            case "couple" -> validateCouple(c, m, path, depth, designW, designH);
            case "frame" -> validateFrame(c, m, path, depth, parentType, coupleDir, designW, designH);
            case "split" -> validateSplit(c, m, path, depth, designW, designH);
            case "sash" -> validateSash(c, m, path, depth, designW, designH);
            case "slide" -> validateSlide(c, m, path, depth, designW, designH);
            case "glass", "panel" -> {
                Object glass = m.get("glass");
                if (glass != null && (!(glass instanceof String g) || g.length() > 64)) {
                    c.err(path + ".glass", "En fazla 64 karakterlik metin olmalı.");
                }
                Object duplex = m.get("duplex");
                if (duplex != null) {
                    if (!(duplex instanceof List<?> d) || d.size() != 2) {
                        c.err(path + ".duplex", "[sütun, satır] olmalı.");
                    } else {
                        for (int i = 0; i < 2; i++) {
                            if (!(d.get(i) instanceof Number n) || n.doubleValue() != Math.floor(n.doubleValue())
                                    || n.intValue() < 1 || n.intValue() > MAX_DUPLEX) {
                                c.err(path + ".duplex[" + i + "]", "1-" + MAX_DUPLEX + " arası tam sayı olmalı.");
                            }
                        }
                    }
                }
            }
            default -> c.err(path + ".type", "Bilinmeyen düğüm tipi: " + t);
        }
    }

    private void validateCouple(Ctx c, Map<?, ?> m, String path, int depth, Double designW, Double designH) {
        if (depth != 1) {
            c.err(path, "couple yalnızca kökte olabilir.");
        }
        Object dir = m.get("dir");
        if (!"V".equals(dir) && !"H".equals(dir)) c.err(path + ".dir", "V veya H olmalı.");
        List<?> children = list(c, m.get("children"), path + ".children");
        List<?> sizes = list(c, m.get("sizes"), path + ".sizes");
        if (children == null || sizes == null) return;
        if (children.size() < 2 || children.size() > MAX_COUPLE_FRAMES) {
            c.err(path + ".children", "2-" + MAX_COUPLE_FRAMES + " kasa olmalı.");
        }
        if (sizes.size() != children.size()) {
            c.err(path + ".sizes", "Parça sayısı ile aynı uzunlukta olmalı.");
        }
        for (int i = 0; i < sizes.size(); i++) {
            if (!(sizes.get(i) instanceof Number n) || !(n.doubleValue() > 0) || Double.isInfinite(n.doubleValue())) {
                c.err(path + ".sizes[" + i + "]", "Pozitif sayı olmalı.");
            }
        }
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (!(child instanceof Map<?, ?> cm) || !"frame".equals(cm.get("type"))) {
                c.err(path + ".children[" + i + "]", "couple çocuğu frame olmalı.");
            }
            node(c, child, path + ".children[" + i + "]", depth + 1, "couple",
                    dir instanceof String d ? d : null, designW, designH);
        }
    }

    private void validateFrame(Ctx c, Map<?, ?> m, String path, int depth, String parentType, String coupleDir,
                               Double designW, Double designH) {
        if (!"couple".equals(parentType)) {
            c.err(path, "frame yalnızca couple çocuğu olabilir.");
        }
        Object bottom = m.get("bottom");
        if (bottom != null && !"frame".equals(bottom) && !"threshold".equals(bottom)) {
            c.err(path + ".bottom", "frame veya threshold olmalı.");
        }
        if ("V".equals(coupleDir) && m.containsKey("height")) {
            Object h = m.get("height");
            if (!(h instanceof Number n) || n.doubleValue() <= 0) {
                c.err(path + ".height", "Pozitif sayı olmalı.");
            } else if (designH != null && n.doubleValue() > designH) {
                c.err(path + ".height", "Tasarım yüksekliğini aşamaz.");
            }
        }
        if ("H".equals(coupleDir) && m.containsKey("width")) {
            Object w = m.get("width");
            if (!(w instanceof Number n) || n.doubleValue() <= 0) {
                c.err(path + ".width", "Pozitif sayı olmalı.");
            } else if (designW != null && n.doubleValue() > designW) {
                c.err(path + ".width", "Tasarım genişliğini aşamaz.");
            }
        }
        if (m.containsKey("shape")) {
            requireSchema2ForShape(c);
            Double fw = numberOr(m.get("width"), designW);
            Double fh = numberOr(m.get("height"), designH);
            validateShape(c, m, path, fw, fh);
        }
        Object child = m.get("child");
        if (child == null) c.err(path + ".child", "Eksik.");
        else node(c, child, path + ".child", depth + 1, "frame", null, designW, designH);
    }

    private void requireSchema2ForShape(Ctx c) {
        if (c.schemaVersion < 2) {
            c.err("$.schemaVersion", "Şekil için schemaVersion 2 gerekli.");
        }
    }

    private void validateShape(Ctx c, Map<?, ?> m, String path, Double width, Double height) {
        Object shape = m.get("shape");
        if (!(shape instanceof Map<?, ?> sm)) {
            c.err(path + ".shape", "Nesne olmalı.");
            return;
        }
        Object type = sm.get("type");
        if (!(type instanceof String st) || !SHAPE_TYPES.contains(st)) {
            c.err(path + ".shape.type", "Şunlardan biri olmalı: " + new TreeSet<>(SHAPE_TYPES));
            return;
        }
        switch (st) {
            case "rect" -> { }
            case "arch" -> {
                if (width == null || height == null) return;
                double r = width / 2.0;
                if (height < r + 200) {
                    c.err(path + ".shape", "Kemer yüksekliği en az yarıçap + 200 mm olmalı.");
                }
            }
            case "segmentArch" -> {
                Object rise = sm.get("rise");
                if (!(rise instanceof Number rn) || Double.isNaN(rn.doubleValue())) {
                    c.err(path + ".shape.rise", "Sayı olmalı.");
                } else if (width != null) {
                    double maxRise = width / 2.0;
                    if (rn.doubleValue() < 50 || rn.doubleValue() > maxRise) {
                        c.err(path + ".shape.rise", "50 ile genişlik/2 arasında olmalı.");
                    }
                }
            }
            case "trapezoid" -> {
                Object lh = sm.get("leftHeight");
                Object rh = sm.get("rightHeight");
                if (!(lh instanceof Number ln) || ln.doubleValue() < 300) {
                    c.err(path + ".shape.leftHeight", "En az 300 mm olmalı.");
                }
                if (!(rh instanceof Number rn) || rn.doubleValue() < 300) {
                    c.err(path + ".shape.rightHeight", "En az 300 mm olmalı.");
                }
                if (lh instanceof Number ln2 && rh instanceof Number rn2 && height != null) {
                    double maxSide = Math.max(ln2.doubleValue(), rn2.doubleValue());
                    if (Math.abs(maxSide - height) > 0.01) {
                        c.err(path + ".shape", "max(sol, sağ) yükseklik tasarım/kasa yüksekliğine eşit olmalı.");
                    }
                }
            }
            case "triangle" -> {
                Object apex = sm.get("apex");
                if (!(apex instanceof String a) || !TRIANGLE_APEX.contains(a)) {
                    c.err(path + ".shape.apex", "center, left veya right olmalı.");
                }
                if (height != null && height < 300) {
                    c.err(path + ".shape", "Üçgen yüksekliği en az 300 mm olmalı.");
                }
            }
            default -> { }
        }
    }

    private static Double numberOr(Object v, Double fallback) {
        return v instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : fallback;
    }

    private void validateSplit(Ctx c, Map<?, ?> m, String path, int depth, Double designW, Double designH) {
        Object dir = m.get("dir");
        if (!"V".equals(dir) && !"H".equals(dir)) c.err(path + ".dir", "V veya H olmalı.");
        Object joint = m.get("joint");
        if (joint != null && !"mullion".equals(joint) && !"adapter".equals(joint)) {
            c.err(path + ".joint", "mullion veya adapter olmalı.");
        }
        boolean adapter = "adapter".equals(joint);
        List<?> children = list(c, m.get("children"), path + ".children");
        List<?> sizes = list(c, m.get("sizes"), path + ".sizes");
        if (children == null || sizes == null) return;
        if (adapter) {
            if (!"V".equals(dir)) c.err(path + ".dir", "Adaptör birleşiminde V olmalı.");
            if (children.size() != 2) {
                c.err(path + ".children", "Adaptör birleşiminde tam 2 çocuk olmalı.");
            }
        } else if (children.size() < 2 || children.size() > MAX_SPLIT_PARTS) {
            c.err(path + ".children", "2-" + MAX_SPLIT_PARTS + " parça olmalı.");
        }
        if (sizes.size() != children.size()) {
            c.err(path + ".sizes", "Parça sayısı ile aynı uzunlukta olmalı.");
        }
        for (int i = 0; i < sizes.size(); i++) {
            if (!(sizes.get(i) instanceof Number n) || !(n.doubleValue() > 0) || Double.isInfinite(n.doubleValue())) {
                c.err(path + ".sizes[" + i + "]", "Pozitif sayı olmalı.");
            }
        }
        if (adapter && children.size() == 2) {
            int passiveCount = 0;
            for (int i = 0; i < 2; i++) {
                Object child = children.get(i);
                if (!(child instanceof Map<?, ?> cm) || !"sash".equals(cm.get("type"))) {
                    c.err(path + ".children[" + i + "]", "Adaptör çocuğu sash olmalı.");
                } else if (bool(cm.get("passive"))) {
                    passiveCount++;
                }
            }
            if (passiveCount != 1) {
                c.err(path + ".children", "Adaptörde tam biri passive:true olmalı.");
            }
        }
        for (int i = 0; i < children.size(); i++) {
            node(c, children.get(i), path + ".children[" + i + "]", depth + 1, "split", null, designW, designH);
        }
    }

    private void validateSlide(Ctx c, Map<?, ?> m, String path, int depth, Double designW, Double designH) {
        if (m.containsKey("panels")) {
            Object system = m.get("system");
            String sys = system instanceof String s && SLIDE_SYSTEMS.contains(s) ? s : null;
            if (sys == null) {
                c.err(path + ".system", "standard veya liftSlide olmalı.");
                sys = "standard";
            }
            List<?> panels = list(c, m.get("panels"), path + ".panels");
            if (panels == null) return;
            int maxPanels = "liftSlide".equals(sys) ? 4 : 6;
            if (panels.size() < 2 || panels.size() > maxPanels) {
                c.err(path + ".panels", "liftSlide".equals(sys)
                        ? "Kaldır-sür için 2-4 panel olmalı."
                        : "Sürme için 2-6 panel olmalı.");
            }
            int movable = 0;
            for (int i = 0; i < panels.size(); i++) {
                Object raw = panels.get(i);
                String pp = path + ".panels[" + i + "]";
                if (!(raw instanceof Map<?, ?> panel)) {
                    c.err(pp, "Panel nesne olmalı.");
                    continue;
                }
                Object move = panel.get("move");
                if (!(move instanceof String mv) || !SLIDE_MOVES.contains(mv)) {
                    c.err(pp + ".move", "left, right veya fixed olmalı.");
                } else if (!"fixed".equals(mv)) {
                    movable++;
                }
                validateScreen(c, panel.get("screen"), pp + ".screen");
                Object child = panel.get("child");
                if (child == null) c.err(pp + ".child", "Eksik.");
                else node(c, child, pp + ".child", depth + 1, "slide", null, designW, designH);
            }
            if (!panels.isEmpty() && movable == 0) {
                c.err(path + ".panels", "En az bir hareketli panel (move ≠ fixed) olmalı.");
            }
            return;
        }
        List<?> children = list(c, m.get("children"), path + ".children");
        if (children == null) return;
        if (children.size() != 2) c.err(path + ".children", "Sürme için tam 2 kanat olmalı.");
        for (int i = 0; i < children.size(); i++) {
            node(c, children.get(i), path + ".children[" + i + "]", depth + 1, "slide", null, designW, designH);
        }
    }

    private void validateSash(Ctx c, Map<?, ?> m, String path, int depth, Double designW, Double designH) {
        Object kind = m.get("kind");
        if (kind != null && !"window".equals(kind) && !"door".equals(kind)) {
            c.err(path + ".kind", "window veya door olmalı.");
        }
        boolean door = "door".equals(kind);
        Object open = m.get("open");
        Set<String> allowed = door ? DOOR_OPENINGS : OPENINGS;
        if (!(open instanceof String o) || !allowed.contains(o)) {
            c.err(path + ".open", "Şunlardan biri olmalı: " + new TreeSet<>(allowed));
        }
        Object passive = m.get("passive");
        if (passive != null && !(passive instanceof Boolean)) {
            c.err(path + ".passive", "Boolean olmalı.");
        }
        Object outward = m.get("outward");
        if (outward != null && !(outward instanceof Boolean)) {
            c.err(path + ".outward", "Boolean olmalı.");
        }
        validateScreen(c, m.get("screen"), path + ".screen");
        Object child = m.get("child");
        if (child == null) c.err(path + ".child", "Eksik.");
        else node(c, child, path + ".child", depth + 1, "sash", null, designW, designH);
    }

    private static void validateScreen(Ctx c, Object screen, String path) {
        if (screen == null) return;
        if (!(screen instanceof String s) || !SCREENS.contains(s)) {
            c.err(path, "plise, hinged veya sliding olmalı.");
        }
    }

    private static boolean bool(Object v) {
        return v instanceof Boolean b && b;
    }

    private static Double asPositiveNumber(Object v) {
        return v instanceof Number n && n.doubleValue() > 0 ? n.doubleValue() : null;
    }

    private static void number(Ctx c, Map<String, Object> m, String key, String path, double min, double max) {
        Object v = m.get(key);
        if (!(v instanceof Number n) || n.doubleValue() < min || n.doubleValue() > max || Double.isNaN(n.doubleValue())) {
            c.err(path + "." + key, min + " ile " + max + " mm arasında olmalı.");
        }
    }

    private static List<?> list(Ctx c, Object v, String path) {
        if (!(v instanceof List<?> l)) {
            c.err(path, "Liste olmalı.");
            return null;
        }
        return l;
    }

    private static final class Ctx {
        final List<Map<String, String>> errors = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        int nodes = 0;
        int schemaVersion = 0;

        void err(String path, String message) {
            Map<String, String> e = new LinkedHashMap<>();
            e.put("path", path);
            e.put("message", message);
            errors.add(e);
        }
    }
}
