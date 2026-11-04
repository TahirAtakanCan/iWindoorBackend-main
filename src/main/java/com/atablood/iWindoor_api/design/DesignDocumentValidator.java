package com.atablood.iWindoor_api.design;

import com.atablood.iWindoor_api.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tasarım belgesinin (Dart: WindowDesign.toJson) yapısal kontrolü.
 * Geometri hesabı istemcide yapılır; sunucu sadece belgenin bozuk olmadığından emin olur.
 *
 * <pre>
 * {"schemaVersion":1,"width":1800,"height":1700,"series":"12","color":"white",
 *  "root":{"id":"n1","type":"split","dir":"V","sizes":[600,600,600],"children":[...]}}
 * düğüm tipleri: split(dir,sizes,children) | sash(open,child) | slide(children[2]) | glass(glass,duplex) | panel(glass,duplex)
 * </pre>
 */
@Component
public class DesignDocumentValidator {

    public static final int SCHEMA_VERSION = 1;
    public static final double MIN_SIZE_MM = 200, MAX_WIDTH_MM = 12000, MAX_HEIGHT_MM = 6000;
    public static final int MAX_DEPTH = 16, MAX_NODES = 400, MAX_SPLIT_PARTS = 12, MAX_DUPLEX = 12;
    public static final Set<String> OPENINGS = Set.of("turnLeft", "turnRight", "tilt", "tiltTurnLeft", "tiltTurnRight");

    /** Geçersizse BadRequestException (code=INVALID_DESIGN, errors=[{path,message}]) fırlatır. */
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
        if (!(v instanceof Number n) || n.intValue() != SCHEMA_VERSION) {
            c.err("$.schemaVersion", "Desteklenen sürüm: " + SCHEMA_VERSION);
        }
        number(c, doc, "width", "$", MIN_SIZE_MM, MAX_WIDTH_MM);
        number(c, doc, "height", "$", MIN_SIZE_MM, MAX_HEIGHT_MM);
        Object series = doc.get("series");
        if (series != null && !(series instanceof String) && !(series instanceof Number)) {
            c.err("$.series", "Metin veya sayı olmalı.");
        }
        Object color = doc.get("color");
        if (color != null && !(color instanceof String)) c.err("$.color", "Metin olmalı.");
        Object root = doc.get("root");
        if (!(root instanceof Map<?, ?>)) {
            c.err("$.root", "Kök düğüm eksik.");
        } else {
            node(c, root, "$.root", 1);
        }
        return c.errors;
    }

    private void node(Ctx c, Object raw, String path, int depth) {
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
            case "split" -> {
                Object dir = m.get("dir");
                if (!"V".equals(dir) && !"H".equals(dir)) c.err(path + ".dir", "V veya H olmalı.");
                List<?> children = list(c, m.get("children"), path + ".children");
                List<?> sizes = list(c, m.get("sizes"), path + ".sizes");
                if (children == null || sizes == null) return;
                if (children.size() < 2 || children.size() > MAX_SPLIT_PARTS) {
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
                for (int i = 0; i < children.size(); i++) node(c, children.get(i), path + ".children[" + i + "]", depth + 1);
            }
            case "sash" -> {
                Object open = m.get("open");
                if (!(open instanceof String o) || !OPENINGS.contains(o)) {
                    c.err(path + ".open", "Şunlardan biri olmalı: " + new TreeSet<>(OPENINGS));
                }
                Object child = m.get("child");
                if (child == null) c.err(path + ".child", "Eksik.");
                else node(c, child, path + ".child", depth + 1);
            }
            case "slide" -> {
                List<?> children = list(c, m.get("children"), path + ".children");
                if (children == null) return;
                if (children.size() != 2) c.err(path + ".children", "Sürme için tam 2 kanat olmalı.");
                for (int i = 0; i < children.size(); i++) node(c, children.get(i), path + ".children[" + i + "]", depth + 1);
            }
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

        void err(String path, String message) {
            Map<String, String> e = new LinkedHashMap<>();
            e.put("path", path);
            e.put("message", message);
            errors.add(e);
        }
    }
}
