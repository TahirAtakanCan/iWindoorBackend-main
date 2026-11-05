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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class CatalogAdminIntegrationTest {

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
                        .content("{\"fullName\":\"Admin\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
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
    void accessoryCrud_priceAdjust_glassAffectsQuote_rolesAndHardwareDelete() throws Exception {
        String admin = register();
        String other = register();

        // Aksesuar oluştur + CODE_TAKEN
        String code = "AKS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        call(post("/api/v1/catalog/accessories"), admin,
                "{\"code\":\"" + code + "\",\"name\":\"Test Aks\",\"unit\":\"PIECE\",\"price\":100,\"isActive\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));
        call(get("/api/v1/catalog/accessories"), admin, null)
                .andExpect(jsonPath("$[*].code", hasItem(code)));
        call(post("/api/v1/catalog/accessories"), admin,
                "{\"code\":\"" + code + "\",\"name\":\"Dup\",\"unit\":\"PIECE\",\"price\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODE_TAKEN"));

        // Cam fiyatı değişince teklif değişir
        String snap = call(get("/api/v1/catalog/snapshot"), admin, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Object seriesObj = JsonPath.read(snap, "$.series[0].id");
        String series = String.valueOf(seriesObj);
        List<Number> glassIds = JsonPath.read(snap, "$.glasses[?(@.code=='4+16+4')].id");
        String glassId = String.valueOf(glassIds.get(0));
        String project = str(call(post("/api/v1/projects"), admin, "{\"name\":\"Kat\"}"), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), admin,
                "{\"name\":\"P\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}"),
                "$.id");
        call(put("/api/v1/windows/" + window), admin,
                "{\"name\":\"P\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(jsonPath("$.unitPrice").value(4816.31));

        call(put("/api/v1/catalog/glasses/" + glassId), admin,
                "{\"name\":\"Seffaf\",\"thicknessMm\":24,\"visualStyle\":\"clear\",\"pricePerSquareMeter\":700,\"isActive\":true}")
                .andExpect(status().isOk());
        call(get("/api/v1/windows/" + window + "/quote"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.not(4816.31)));

        // price-adjustment ACCESSORY %10 dryRun + gerçek
        ResultActions dry = call(post("/api/v1/catalog/price-adjustment").param("dryRun", "true"), admin,
                "{\"target\":\"ACCESSORY\",\"percent\":10}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated", greaterThan(0)));
        Object dryUpdated = JsonPath.read(dry.andReturn().getResponse().getContentAsString(), "$.updated");
        int dryCount = ((Number) dryUpdated).intValue();

        String accJson = call(get("/api/v1/catalog/accessories").param("includeInactive", "true"), admin, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> kolIds = JsonPath.read(accJson, "$[?(@.code=='KOL-STD')].id");
        String kolId = String.valueOf(kolIds.get(0));
        List<Number> kolPrices = JsonPath.read(accJson, "$[?(@.code=='KOL-STD')].price");
        org.junit.jupiter.api.Assertions.assertEquals(250, kolPrices.get(0).doubleValue(), 0.001);

        call(post("/api/v1/catalog/price-adjustment"), admin, "{\"target\":\"ACCESSORY\",\"percent\":10}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(dryCount));
        String accAfter = call(get("/api/v1/catalog/accessories").param("includeInactive", "true"), admin, null)
                .andReturn().getResponse().getContentAsString();
        List<Number> kolAfter = JsonPath.read(accAfter, "$[?(@.code=='KOL-STD')].price");
        org.junit.jupiter.api.Assertions.assertEquals(275.0, kolAfter.get(0).doubleValue(), 0.001);

        // Başka şirket / çalışan 403
        call(put("/api/v1/catalog/accessories/" + kolId), other,
                "{\"code\":\"KOL-STD\",\"name\":\"x\",\"unit\":\"PIECE\",\"price\":1}")
                .andExpect(status().isForbidden());

        String empEmail = UUID.randomUUID().toString().substring(0, 8) + "@emp.test";
        call(post("/api/v1/users"), admin,
                "{\"fullName\":\"Emp\",\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}")
                .andExpect(status().isOk());
        String empToken = JsonPath.read(mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        call(put("/api/v1/catalog/accessories/" + kolId), empToken,
                "{\"code\":\"KOL-STD\",\"name\":\"x\",\"unit\":\"PIECE\",\"price\":1}")
                .andExpect(status().isForbidden());

        // Donanım kuralı silme: TILT_TURN CIFT-ACILIM-SET silinince aynı BOM daha ucuz
        String bomTrim = TestData.BOM.trim();
        String bomHw = bomTrim.substring(0, bomTrim.length() - 1)
                + ",{\"group\":\"HARDWARE\",\"kind\":\"TILT_TURN\",\"w\":536,\"h\":1214,\"qty\":1}]";

        String wBefore = str(call(post("/api/v1/projects/" + project + "/windows"), admin,
                "{\"name\":\"P2\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}"),
                "$.id");
        String beforeBody = call(put("/api/v1/windows/" + wBefore), admin,
                "{\"name\":\"P2\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + bomHw + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        double priceWithCift = ((Number) JsonPath.read(beforeBody, "$.unitPrice")).doubleValue();

        String rulesJson = call(get("/api/v1/catalog/hardware-rules"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(rulesJson,
                "$.[?(@.kind=='TILT_TURN' && @.accessoryCode=='CIFT-ACILIM-SET')].id");
        org.junit.jupiter.api.Assertions.assertFalse(ids.isEmpty());
        call(delete("/api/v1/catalog/hardware-rules/" + ids.get(0)), admin, null).andExpect(status().isOk());

        String wAfter = str(call(post("/api/v1/projects/" + project + "/windows"), admin,
                "{\"name\":\"P3\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}"),
                "$.id");
        call(put("/api/v1/windows/" + wAfter), admin,
                "{\"name\":\"P3\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + bomHw + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(org.hamcrest.Matchers.lessThan(priceWithCift)));
    }
}
