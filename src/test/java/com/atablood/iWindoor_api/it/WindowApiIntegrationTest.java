package com.atablood.iWindoor_api.it;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uçtan uca: kayıt -> katalog -> proje -> tasarımlı pencere -> kesim listesiyle fiyat -> çakışma -> yetki.
 * PostgreSQL ister. Çalıştırma: IWINDOOR_IT=true ./mvnw test
 * (veritabanı: IWINDOOR_TEST_DB_URL, varsayılan jdbc:postgresql://localhost:5432/windoor_test)
 */
@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class WindowApiIntegrationTest {

    private static final String DESIGN = """
            {"schemaVersion":1,"width":1800,"height":1700,"series":"%s","color":"white","root":
             {"id":"n1","type":"split","dir":"H","sizes":[400,1300],"children":[
               {"id":"n2","type":"split","dir":"V","sizes":[600,600,600],"children":[
                 {"id":"n3","type":"glass","glass":"4+16+4"},{"id":"n4","type":"glass","glass":"4+16+4"},{"id":"n5","type":"glass","glass":"4+16+4"}]},
               {"id":"n6","type":"split","dir":"V","sizes":[600,600,600],"children":[
                 {"id":"n7","type":"glass","glass":"4+16+4"},
                 {"id":"n8","type":"sash","open":"tiltTurnLeft","child":{"id":"n9","type":"glass","glass":"4+16+4"}},
                 {"id":"n10","type":"glass","glass":"4+16+4"}]}]}}""";

    private static final String BOM = """
            [{"group":"PROFILE","role":"FRAME","length":1806,"qty":2},{"group":"PROFILE","role":"FRAME","length":1706,"qty":2},
             {"group":"PROFILE","role":"MULLION","length":1682,"qty":1},{"group":"PROFILE","role":"MULLION","length":1204,"qty":2},
             {"group":"PROFILE","role":"MULLION","length":304,"qty":2},
             {"group":"PROFILE","role":"SASH","length":542,"qty":2},{"group":"PROFILE","role":"SASH","length":1220,"qty":2},
             {"group":"BEAD","length":1198,"qty":4,"glassCode":"4+16+4"},{"group":"BEAD","length":1058,"qty":2,"glassCode":"4+16+4"},
             {"group":"BEAD","length":520,"qty":2,"glassCode":"4+16+4"},{"group":"BEAD","length":498,"qty":8,"glassCode":"4+16+4"},
             {"group":"BEAD","length":380,"qty":2,"glassCode":"4+16+4"},{"group":"BEAD","length":298,"qty":6,"glassCode":"4+16+4"},
             {"group":"GLASS","glassCode":"4+16+4","w":548,"h":326,"qty":1},{"group":"GLASS","glassCode":"4+16+4","w":526,"h":326,"qty":2},
             {"group":"GLASS","glassCode":"4+16+4","w":526,"h":1226,"qty":2},{"group":"GLASS","glassCode":"4+16+4","w":408,"h":1086,"qty":1}]""";

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String register() throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String body = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String token,
                               String json) throws Exception {
        b.header("Authorization", "Bearer " + token);
        if (json != null) b.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(b);
    }

    private static String str(ResultActions r, String path) throws Exception {
        Object v = JsonPath.read(r.andReturn().getResponse().getContentAsString(), path);
        return String.valueOf(v);
    }

    @Test
    void fullDesignAndPricingFlow() throws Exception {
        String t1 = register(), t2 = register();

        ResultActions snap = call(get("/api/v1/catalog/snapshot"), t1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].geometry.frameFace").value(62.0))
                .andExpect(jsonPath("$.series[0].geometry.glassRebateSash").value(14.0));
        String series = str(snap, "$.series[0].id");

        String project = str(call(post("/api/v1/projects"), t1, "{\"name\":\"Yayla Evi\"}").andExpect(status().isOk()), "$.id");
        String design = DESIGN.formatted(series);

        ResultActions created = call(post("/api/v1/projects/" + project + "/windows"), t1,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacyDesign").value(false))
                .andExpect(jsonPath("$.rowVersion").value(0));
        String window = str(created, "$.id");

        String update = "{\"name\":\"Salon\",\"quantity\":2,\"design\":" + design + ",\"bom\":" + BOM + ",\"rowVersion\":0}";
        call(put("/api/v1/windows/" + window), t1, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(4816.31))
                .andExpect(jsonPath("$.rowVersion").value(1));

        call(get("/api/v1/projects/" + project), t1, null)
                .andExpect(jsonPath("$.totalPrice").value(9632.62))
                .andExpect(jsonPath("$.windowUnits.length()").value(1));

        call(get("/api/v1/windows/" + window + "/quote"), t1, null)
                .andExpect(jsonPath("$.total").value(9632.62))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        // aynı sürümle ikinci kayıt -> çakışma
        call(put("/api/v1/windows/" + window), t1, update)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        // başka şirket
        call(get("/api/v1/windows/" + window), t2, null).andExpect(status().isForbidden());
        call(get("/api/v1/projects/" + project), t2, null).andExpect(status().isForbidden());

        // bozuk belge
        call(put("/api/v1/windows/" + window), t1,
                "{\"name\":\"Salon\",\"design\":{\"schemaVersion\":1,\"width\":1800,\"height\":1700,\"root\":{\"id\":\"a\",\"type\":\"door\"}},\"rowVersion\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DESIGN"))
                .andExpect(jsonPath("$.errors[0].path").value("$.root.type"));

        // token yok
        mvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void legacyEditorStillWorks() throws Exception {
        String t = register();
        String project = str(call(post("/api/v1/projects"), t, "{\"name\":\"Eski akış\",\"description\":\"\"}"), "$.id");
        call(post("/api/v1/projects/" + project + "/windows"), t, "{\"name\":\"P1\",\"width\":1200,\"height\":1400}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacyDesign").value(true));
        ResultActions p = call(get("/api/v1/projects/" + project), t, null).andExpect(status().isOk());
        String root = str(p, "$.windowUnits[0].rootNode.id");
        String window = str(p, "$.windowUnits[0].id");

        call(post("/api/v1/design/split/" + root).param("isVertical", "true"), t, null).andExpect(status().isOk());
        call(get("/api/v1/windows/" + window), t, null)
                .andExpect(jsonPath("$.design.root.type").value("split"))
                .andExpect(jsonPath("$.design.root.children.length()").value(2));

        // başka şirketin düğümü bölünemez
        String other = register();
        call(post("/api/v1/design/split/" + root).param("isVertical", "false"), other, null).andExpect(status().isForbidden());

        call(post("/api/v1/projects/" + project + "/calculate-price"), t, null).andExpect(status().isOk());
        call(get("/api/v1/projects/" + project + "/cost-summary"), t, null).andExpect(status().isOk());
        call(put("/api/v1/projects/" + project), t, "{\"name\":\"Yeni ad\",\"description\":\"x\"}")
                .andExpect(jsonPath("$.name").value("Yeni ad"));
    }
}
