package com.atablood.iWindoor_api.design;

import com.atablood.iWindoor_api.entity.Glass;
import com.atablood.iWindoor_api.entity.NodeType;
import com.atablood.iWindoor_api.entity.WindowNode;
import com.atablood.iWindoor_api.entity.WindowUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DesignDocumentsTest {

    private static WindowNode node(long id, NodeType t, double w, double h, int order) {
        WindowNode n = new WindowNode();
        n.setId(id);
        n.setNodeType(t);
        n.setWidth(w);
        n.setHeight(h);
        n.setItemOrder(order);
        return n;
    }

    @Test
    void convertsLegacyTreeIntoValidDocument() {
        WindowNode root = node(1, NodeType.MULLION_VERTICAL, 1200, 1400, 0);
        WindowNode right = node(3, NodeType.SASH, 700, 1400, 1);
        WindowNode left = node(2, NodeType.GLASS, 500, 1400, 0);
        Glass g = new Glass();
        g.setCode("4+16+4");
        left.setGlass(g);
        root.addChild(right); // ters sırada eklendi: itemOrder'a göre sıralanmalı
        root.addChild(left);

        WindowUnit unit = new WindowUnit();
        unit.setWidth(1200.0);
        unit.setHeight(1400.0);
        unit.setRootNode(root);

        Map<String, Object> doc = DesignDocuments.fromLegacy(unit);
        assertThat(new DesignDocumentValidator().validate(doc)).isEmpty();

        @SuppressWarnings("unchecked") Map<String, Object> r = (Map<String, Object>) doc.get("root");
        assertThat(r.get("type")).isEqualTo("split");
        assertThat(r.get("dir")).isEqualTo("V");
        assertThat(r.get("sizes")).isEqualTo(List.of(500.0, 700.0));
        @SuppressWarnings("unchecked") List<Map<String, Object>> kids = (List<Map<String, Object>>) r.get("children");
        assertThat(kids.get(0).get("glass")).isEqualTo("4+16+4");
        assertThat(kids.get(1).get("type")).isEqualTo("sash");
        assertThat(kids.get(1).get("open")).isEqualTo("turnLeft");
    }

    @Test
    void singleGlassIsValid() {
        assertThat(new DesignDocumentValidator().validate(DesignDocuments.singleGlass(1000, 1200, 5L, "4"))).isEmpty();
    }
}
