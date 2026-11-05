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
class HardwarePricingIntegrationTest {

    private static final String HARDWARE = """
            {"group":"HARDWARE","kind":"TILT_TURN","w":536,"h":1214,"qty":1}""";

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
    void hardwareBomPricesAndRulesAreProvisioned() throws Exception {
        String token = register();

        call(get("/api/v1/catalog/hardware-rules"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[*].kind", hasItem("TILT_TURN")))
                .andExpect(jsonPath("$[*].accessoryCode", hasItem("CIFT-ACILIM-SET")));

        String series = str(call(get("/api/v1/catalog/snapshot"), token, null).andExpect(status().isOk()), "$.series[0].id");
        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Donanım\"}").andExpect(status().isOk()), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");

        String bomTrim = TestData.BOM.trim();
        String bomWithHw = bomTrim.substring(0, bomTrim.length() - 1) + "," + HARDWARE + "]";
        call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"Salon\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + bomWithHw + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(5758.31));

        call(get("/api/v1/projects/" + project + "/quote"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windows[0].unitPrice").value(5758.31))
                .andExpect(jsonPath("$.materials[?(@.group=='ACCESSORY')].code", hasItem("KOL-STD")))
                .andExpect(jsonPath("$.materials[?(@.group=='ACCESSORY')].code", hasItem("CIFT-ACILIM-SET")))
                .andExpect(jsonPath("$.materials[?(@.group=='ACCESSORY')].code", hasItem("CONTA-EPDM")));
    }
}
