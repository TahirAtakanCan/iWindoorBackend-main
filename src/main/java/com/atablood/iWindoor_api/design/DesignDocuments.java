package com.atablood.iWindoor_api.design;

import com.atablood.iWindoor_api.entity.Glass;
import com.atablood.iWindoor_api.entity.NodeType;
import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.entity.WindowUnit;

import java.util.*;

/** Tasarım belgesi üretme yardımcıları: varsayılan belge ve eski düğüm ağacından dönüşüm. */
public final class DesignDocuments {

    private DesignDocuments() {
    }

    /** Tek camlı sabit pencere. */
    public static Map<String, Object> singleGlass(double width, double height, Long seriesId, String glassCode) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("id", "n1");
        leaf.put("type", "glass");
        leaf.put("glass", glassCode == null ? "" : glassCode);
        leaf.put("duplex", List.of(1, 1));
        return document(width, height, seriesId, leaf);
    }

    public static Map<String, Object> document(double width, double height, Long seriesId, Map<String, Object> root) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", DesignDocumentValidator.SCHEMA_VERSION);
        doc.put("width", width);
        doc.put("height", height);
        doc.put("series", seriesId == null ? null : String.valueOf(seriesId));
        doc.put("color", "white");
        doc.put("root", root);
        return doc;
    }

    /**
     * Eski sunucu ağacını (window_nodes) belgeye çevirir.
     * FRAME yaprağı -> cam, MULLION_VERTICAL/HORIZONTAL -> split V/H (boyutlar çocukların ölçüsünden),
     * SASH -> sash(turnLeft) + cam, GLASS/EMPTY -> cam. Açılım yönü eski modelde olmadığı için turnLeft varsayılır.
     */
    public static Map<String, Object> fromLegacy(WindowUnit unit) {
        double w = unit.getWidth() == null ? 1000 : unit.getWidth();
        double h = unit.getHeight() == null ? 1000 : unit.getHeight();
        Long seriesId = unit.getSeriesId();
        if (unit.getRootNode() == null) return singleGlass(w, h, seriesId, "");
        return document(w, h, seriesId, convert(unit.getRootNode()));
    }

    private static Map<String, Object> convert(WindowNode n) {
        NodeType t = n.getNodeType() == null ? NodeType.EMPTY : n.getNodeType();
        List<WindowNode> kids = n.getChildren() == null ? List.of() : n.getChildren();
        String id = "L" + n.getId();
        if ((t == NodeType.MULLION_VERTICAL || t == NodeType.MULLION_HORIZONTAL) && kids.size() >= 2) {
            boolean vertical = t == NodeType.MULLION_VERTICAL;
            List<WindowNode> sorted = new ArrayList<>(kids);
            sorted.sort(Comparator.comparing(k -> k.getItemOrder() == null ? 0 : k.getItemOrder()));
            List<Object> sizes = new ArrayList<>();
            List<Object> children = new ArrayList<>();
            for (WindowNode k : sorted) {
                Double s = vertical ? k.getWidth() : k.getHeight();
                sizes.add(s == null || s <= 0 ? 1.0 : s);
                children.add(convert(k));
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("type", "split");
            m.put("dir", vertical ? "V" : "H");
            m.put("sizes", sizes);
            m.put("children", children);
            return m;
        }
        if (!kids.isEmpty() && kids.size() < 2) return convert(kids.get(0));
        if (t == NodeType.SASH) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("type", "sash");
            m.put("open", "turnLeft");
            m.put("child", glassLeaf("L" + n.getId() + "g", n.getGlass()));
            return m;
        }
        return glassLeaf(id, n.getGlass());
    }

    private static Map<String, Object> glassLeaf(String id, Glass glass) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("type", "glass");
        m.put("glass", glass == null || glass.getCode() == null ? "" : glass.getCode());
        m.put("duplex", List.of(1, 1));
        return m;
    }
}
