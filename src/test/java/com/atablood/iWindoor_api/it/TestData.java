package com.atablood.iWindoor_api.it;

/** Entegrasyon testlerinde ortak tasarım belgesi ve kesim listesi örnekleri. */
public final class TestData {

    private TestData() {
    }

    public static final String DESIGN = """
            {"schemaVersion":1,"width":1800,"height":1700,"series":"%s","color":"white","root":
             {"id":"n1","type":"split","dir":"H","sizes":[400,1300],"children":[
               {"id":"n2","type":"split","dir":"V","sizes":[600,600,600],"children":[
                 {"id":"n3","type":"glass","glass":"4+16+4"},{"id":"n4","type":"glass","glass":"4+16+4"},{"id":"n5","type":"glass","glass":"4+16+4"}]},
               {"id":"n6","type":"split","dir":"V","sizes":[600,600,600],"children":[
                 {"id":"n7","type":"glass","glass":"4+16+4"},
                 {"id":"n8","type":"sash","open":"tiltTurnLeft","child":{"id":"n9","type":"glass","glass":"4+16+4"}},
                 {"id":"n10","type":"glass","glass":"4+16+4"}]}]}}""";

    public static final String BOM = """
            [{"group":"PROFILE","role":"FRAME","length":1806,"qty":2},{"group":"PROFILE","role":"FRAME","length":1706,"qty":2},
             {"group":"PROFILE","role":"MULLION","length":1682,"qty":1},{"group":"PROFILE","role":"MULLION","length":1204,"qty":2},
             {"group":"PROFILE","role":"MULLION","length":304,"qty":2},
             {"group":"PROFILE","role":"SASH","length":542,"qty":2},{"group":"PROFILE","role":"SASH","length":1220,"qty":2},
             {"group":"BEAD","length":1198,"qty":4,"glassCode":"4+16+4"},{"group":"BEAD","length":1058,"qty":2,"glassCode":"4+16+4"},
             {"group":"BEAD","length":520,"qty":2,"glassCode":"4+16+4"},{"group":"BEAD","length":498,"qty":8,"glassCode":"4+16+4"},
             {"group":"BEAD","length":380,"qty":2,"glassCode":"4+16+4"},{"group":"BEAD","length":298,"qty":6,"glassCode":"4+16+4"},
             {"group":"GLASS","glassCode":"4+16+4","w":548,"h":326,"qty":1},{"group":"GLASS","glassCode":"4+16+4","w":526,"h":326,"qty":2},
             {"group":"GLASS","glassCode":"4+16+4","w":526,"h":1226,"qty":2},{"group":"GLASS","glassCode":"4+16+4","w":408,"h":1086,"qty":1}]""";
}
