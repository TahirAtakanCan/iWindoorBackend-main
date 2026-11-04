package com.atablood.iWindoor_api.design;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DesignDocumentValidatorTest {

    private final DesignDocumentValidator validator = new DesignDocumentValidator();

    static Map<String, Object> leaf(String id) {
        return new LinkedHashMap<>(Map.of("id", id, "type", "glass", "glass", "4+16+4", "duplex", List.of(1, 1)));
    }

    /** Prototip / Dart testindeki 1800x1700 örnek pencere. */
    static Map<String, Object> sample() {
        Map<String, Object> sash = new LinkedHashMap<>(Map.of("id", "n8", "type", "sash", "open", "tiltTurnLeft", "child", leaf("n9")));
        Map<String, Object> top = new LinkedHashMap<>(Map.of("id", "n2", "type", "split", "dir", "V",
                "sizes", List.of(600, 600, 600), "children", List.of(leaf("n3"), leaf("n4"), leaf("n5"))));
        Map<String, Object> bottom = new LinkedHashMap<>(Map.of("id", "n6", "type", "split", "dir", "V",
                "sizes", List.of(600, 600, 600), "children", List.of(leaf("n7"), sash, leaf("n10"))));
        Map<String, Object> root = new LinkedHashMap<>(Map.of("id", "n1", "type", "split", "dir", "H",
                "sizes", List.of(400, 1300), "children", List.of(top, bottom)));
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", 1);
        doc.put("width", 1800);
        doc.put("height", 1700);
        doc.put("series", "1");
        doc.put("color", "white");
        doc.put("root", root);
        return doc;
    }

    @Test
    void acceptsSampleWindow() {
        assertThat(validator.validate(sample())).isEmpty();
    }

    @Test
    void acceptsSlidingAndPanel() {
        Map<String, Object> panel = new LinkedHashMap<>(Map.of("id", "b", "type", "panel", "glass", "PANEL-24"));
        Map<String, Object> slide = new LinkedHashMap<>(Map.of("id", "s", "type", "slide", "children", List.of(leaf("a"), panel)));
        Map<String, Object> doc = sample();
        doc.put("root", slide);
        assertThat(validator.validate(doc)).isEmpty();
    }

    @Test
    void reportsEveryProblemWithPath() {
        Map<String, Object> doc = sample();
        doc.put("schemaVersion", 2);
        doc.put("width", 50);
        @SuppressWarnings("unchecked") Map<String, Object> root = (Map<String, Object>) doc.get("root");
        root.put("sizes", List.of(400));
        @SuppressWarnings("unchecked") Map<String, Object> bottom = (Map<String, Object>) ((List<?>) root.get("children")).get(1);
        @SuppressWarnings("unchecked") Map<String, Object> sash = (Map<String, Object>) ((List<?>) bottom.get("children")).get(1);
        sash.put("open", "sideways");

        List<String> paths = validator.validate(doc).stream().map(e -> e.get("path")).toList();
        assertThat(paths).containsExactlyInAnyOrder("$.schemaVersion", "$.width", "$.root.sizes",
                "$.root.children[1].children[1].open");
    }

    @Test
    void rejectsDuplicateIdsAndUnknownTypes() {
        Map<String, Object> doc = sample();
        doc.put("root", new LinkedHashMap<>(Map.of("id", "x", "type", "split", "dir", "V", "sizes", List.of(1, 1),
                "children", List.of(leaf("x"), new LinkedHashMap<>(Map.of("id", "y", "type", "door"))))));
        List<String> messages = validator.validate(doc).stream().map(e -> e.get("path") + " " + e.get("message")).toList();
        assertThat(messages).anyMatch(m -> m.contains("Tekrarlanan id"));
        assertThat(messages).anyMatch(m -> m.contains("Bilinmeyen düğüm tipi"));
    }

    @Test
    void rejectsBadDuplex() {
        Map<String, Object> doc = sample();
        Map<String, Object> l = leaf("only");
        l.put("duplex", List.of(0, 2.5));
        doc.put("root", l);
        assertThat(validator.validate(doc)).hasSize(2);
    }
}
