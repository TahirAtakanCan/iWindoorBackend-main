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
        doc.put("schemaVersion", 3);
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
    void acceptsSchemaVersion2AndAdapterSplit() {
        Map<String, Object> active = new LinkedHashMap<>(Map.of(
                "id", "a", "type", "sash", "open", "turnLeft", "passive", false,
                "child", leaf("ag")));
        Map<String, Object> passive = new LinkedHashMap<>(Map.of(
                "id", "p", "type", "sash", "open", "turnRight", "passive", true,
                "child", leaf("pg")));
        Map<String, Object> root = new LinkedHashMap<>(Map.of(
                "id", "r", "type", "split", "dir", "V", "joint", "adapter",
                "sizes", List.of(900, 900), "children", List.of(active, passive)));
        Map<String, Object> doc = sample();
        doc.put("schemaVersion", 2);
        doc.put("bottom", "threshold");
        doc.put("root", root);
        assertThat(validator.validate(doc)).isEmpty();
    }

    @Test
    void rejectsInvalidAdapterAndDoorOpen() {
        Map<String, Object> a = new LinkedHashMap<>(Map.of(
                "id", "a", "type", "sash", "open", "turnLeft", "child", leaf("ag")));
        Map<String, Object> b = new LinkedHashMap<>(Map.of(
                "id", "b", "type", "sash", "open", "turnRight", "child", leaf("bg")));
        Map<String, Object> noPassive = new LinkedHashMap<>(Map.of(
                "id", "r", "type", "split", "dir", "V", "joint", "adapter",
                "sizes", List.of(1, 1), "children", List.of(a, b)));
        Map<String, Object> doc = sample();
        doc.put("root", noPassive);
        assertThat(validator.validate(doc).stream().map(e -> e.get("path"))).contains("$.root.children");

        Map<String, Object> three = new LinkedHashMap<>(Map.of(
                "id", "r2", "type", "split", "dir", "V", "joint", "adapter",
                "sizes", List.of(1, 1, 1),
                "children", List.of(a, b, leaf("x"))));
        doc.put("root", three);
        assertThat(validator.validate(doc).stream().map(e -> e.get("message")))
                .anyMatch(m -> m.contains("Adaptör"));

        Map<String, Object> horizontal = new LinkedHashMap<>(Map.of(
                "id", "r3", "type", "split", "dir", "H", "joint", "adapter",
                "sizes", List.of(1, 1),
                "children", List.of(
                        new LinkedHashMap<>(Map.of("id", "a2", "type", "sash", "open", "turnLeft", "passive", true, "child", leaf("ag2"))),
                        new LinkedHashMap<>(Map.of("id", "b2", "type", "sash", "open", "turnRight", "child", leaf("bg2"))))));
        doc.put("root", horizontal);
        assertThat(validator.validate(doc).stream().map(e -> e.get("path"))).contains("$.root.dir");

        Map<String, Object> door = new LinkedHashMap<>(Map.of(
                "id", "d", "type", "sash", "kind", "door", "open", "tilt", "child", leaf("dg")));
        doc.put("root", door);
        assertThat(validator.validate(doc).stream().map(e -> e.get("path"))).contains("$.root.open");
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

    @Test
    void acceptsAndRejectsShutterAndExtras() {
        Map<String, Object> ok = sample();
        ok.put("schemaVersion", 2);
        ok.put("shutter", Map.of("type", "mono", "slat", "aluminum", "drive", "manual"));
        ok.put("extras", Map.of(
                "sillOuter", true, "sillInner", false, "trim", "three",
                "extension", Map.of("top", 30, "left", 0, "right", 20, "bottom", 0)));
        assertThat(validator.validate(ok)).isEmpty();

        Map<String, Object> badSlat = sample();
        badSlat.put("schemaVersion", 2);
        badSlat.put("shutter", Map.of("type", "mono", "slat", "wood", "drive", "manual"));
        assertThat(validator.validate(badSlat).stream().map(e -> e.get("path")))
                .contains("$.shutter.slat");

        Map<String, Object> badExt = sample();
        badExt.put("schemaVersion", 2);
        badExt.put("extras", Map.of("extension", Map.of("top", 300)));
        assertThat(validator.validate(badExt).stream().map(e -> e.get("path")))
                .contains("$.extras.extension.top");
    }

    @Test
    void acceptsAndRejectsShapedWindows() {
        Map<String, Object> archOk = sample();
        archOk.put("schemaVersion", 2);
        archOk.put("width", 1000);
        archOk.put("height", 1500);
        Map<String, Object> archRoot = leaf("arch");
        archRoot.put("shape", Map.of("type", "arch"));
        archOk.put("root", archRoot);
        assertThat(validator.validate(archOk)).isEmpty();

        Map<String, Object> archBad = sample();
        archBad.put("schemaVersion", 2);
        archBad.put("width", 1000);
        archBad.put("height", 600);
        Map<String, Object> archBadRoot = leaf("archBad");
        archBadRoot.put("shape", Map.of("type", "arch"));
        archBad.put("root", archBadRoot);
        assertThat(validator.validate(archBad)).isNotEmpty();

        Map<String, Object> segBad = sample();
        segBad.put("schemaVersion", 2);
        segBad.put("width", 1000);
        segBad.put("height", 1500);
        Map<String, Object> segRoot = leaf("seg");
        segRoot.put("shape", Map.of("type", "segmentArch", "rise", 600));
        segBad.put("root", segRoot);
        assertThat(validator.validate(segBad)).isNotEmpty();

        Map<String, Object> trapOk = sample();
        trapOk.put("schemaVersion", 2);
        trapOk.put("width", 1200);
        trapOk.put("height", 1500);
        Map<String, Object> trapRoot = leaf("trap");
        trapRoot.put("shape", Map.of("type", "trapezoid", "leftHeight", 1500, "rightHeight", 1000));
        trapOk.put("root", trapRoot);
        assertThat(validator.validate(trapOk)).isEmpty();

        Map<String, Object> trapBad = sample();
        trapBad.put("schemaVersion", 2);
        trapBad.put("width", 1200);
        trapBad.put("height", 1400);
        Map<String, Object> trapBadRoot = leaf("trapBad");
        trapBadRoot.put("shape", Map.of("type", "trapezoid", "leftHeight", 1500, "rightHeight", 1000));
        trapBad.put("root", trapBadRoot);
        assertThat(validator.validate(trapBad)).isNotEmpty();

        Map<String, Object> triBad = sample();
        triBad.put("schemaVersion", 2);
        triBad.put("width", 1000);
        triBad.put("height", 1200);
        Map<String, Object> triRoot = leaf("tri");
        triRoot.put("shape", Map.of("type", "triangle", "apex", "top"));
        triBad.put("root", triRoot);
        assertThat(validator.validate(triBad).stream().map(e -> e.get("path")))
                .contains("$.root.shape.apex");
    }

    @Test
    void acceptsNewSlidePanelsAndScreen() {
        Map<String, Object> panels = new LinkedHashMap<>(Map.of(
                "id", "s1", "type", "slide", "system", "standard",
                "panels", List.of(
                        Map.of("move", "right", "child", leaf("p1")),
                        Map.of("move", "fixed", "child", leaf("p2")),
                        Map.of("move", "left", "screen", "plise", "child", leaf("p3")))));
        Map<String, Object> doc = sample();
        doc.put("root", panels);
        assertThat(validator.validate(doc)).isEmpty();

        Map<String, Object> sash = new LinkedHashMap<>(Map.of(
                "id", "sa", "type", "sash", "open", "turnLeft", "screen", "plise", "child", leaf("sg")));
        doc.put("root", sash);
        assertThat(validator.validate(doc)).isEmpty();

        Map<String, Object> oldSlide = new LinkedHashMap<>(Map.of(
                "id", "old", "type", "slide", "children", List.of(leaf("a"), leaf("b"))));
        doc.put("root", oldSlide);
        assertThat(validator.validate(doc)).isEmpty();
    }

    @Test
    void rejectsInvalidSlidePanelsAndScreen() {
        Map<String, Object> doc = sample();

        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "allFixed", "type", "slide", "system", "standard",
                "panels", List.of(
                        Map.of("move", "fixed", "child", leaf("f1")),
                        Map.of("move", "fixed", "child", leaf("f2"))))));
        assertThat(validator.validate(doc)).isNotEmpty();

        List<Map<String, Object>> seven = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            seven.add(Map.of("move", i == 0 ? "left" : "fixed", "child", leaf("n" + i)));
        }
        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "seven", "type", "slide", "system", "standard", "panels", seven)));
        assertThat(validator.validate(doc)).isNotEmpty();

        List<Map<String, Object>> fiveLift = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fiveLift.add(Map.of("move", i == 0 ? "left" : "fixed", "child", leaf("l" + i)));
        }
        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "lift5", "type", "slide", "system", "liftSlide", "panels", fiveLift)));
        assertThat(validator.validate(doc)).isNotEmpty();

        Map<String, Object> badScreen = new LinkedHashMap<>(Map.of(
                "id", "bs", "type", "sash", "open", "turnLeft", "screen", "abc", "child", leaf("bg")));
        doc.put("root", badScreen);
        assertThat(validator.validate(doc).stream().map(e -> e.get("path"))).contains("$.root.screen");
    }

    @Test
    void acceptsCoupleRootAndRejectsInvalidCouple() {
        Map<String, Object> frame1 = new LinkedHashMap<>(Map.of(
                "id", "f1", "type", "frame", "height", 1400, "child", leaf("g1")));
        Map<String, Object> frame2 = new LinkedHashMap<>(Map.of(
                "id", "f2", "type", "frame", "height", 1700, "child", leaf("g2")));
        Map<String, Object> couple = new LinkedHashMap<>(Map.of(
                "id", "c1", "type", "couple", "dir", "V",
                "sizes", List.of(900, 900), "children", List.of(frame1, frame2)));
        Map<String, Object> doc = sample();
        doc.put("schemaVersion", 2);
        doc.put("root", couple);
        assertThat(validator.validate(doc)).isEmpty();

        Map<String, Object> nestedCouple = new LinkedHashMap<>(Map.of(
                "id", "c2", "type", "couple", "dir", "V",
                "sizes", List.of(1, 1),
                "children", List.of(
                        new LinkedHashMap<>(Map.of("id", "nf1", "type", "frame", "child", leaf("ng1"))),
                        new LinkedHashMap<>(Map.of("id", "nf2", "type", "frame", "child", leaf("ng2"))))));
        Map<String, Object> outerFrame = new LinkedHashMap<>(Map.of(
                "id", "of1", "type", "frame", "child", nestedCouple));
        Map<String, Object> outerFrame2 = new LinkedHashMap<>(Map.of(
                "id", "of2", "type", "frame", "child", leaf("og2")));
        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "c3", "type", "couple", "dir", "V",
                "sizes", List.of(1, 1), "children", List.of(outerFrame, outerFrame2))));
        assertThat(validator.validate(doc)).isNotEmpty();

        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "alone", "type", "frame", "child", leaf("x"))));
        assertThat(validator.validate(doc).stream().map(e -> e.get("message")))
                .anyMatch(m -> m.contains("couple"));

        doc.put("root", new LinkedHashMap<>(Map.of(
                "id", "c4", "type", "couple", "dir", "H",
                "sizes", List.of(900),
                "children", List.of(
                        new LinkedHashMap<>(Map.of("id", "hf1", "type", "frame", "child", leaf("hg1"))),
                        new LinkedHashMap<>(Map.of("id", "hf2", "type", "frame", "child", leaf("hg2")))))));
        assertThat(validator.validate(doc).stream().map(e -> e.get("path"))).contains("$.root.sizes");
    }
}
