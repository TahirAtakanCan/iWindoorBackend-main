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

import static org.hamcrest.Matchers.hasSize;
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
class CouplingAndColorsIntegrationTest {

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
    void snapshotColorsAndSurchargeAffectsQuote() throws Exception {
        String token = register();

        String snap = call(get("/api/v1/catalog/snapshot"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colors", hasSize(6)))
                .andExpect(jsonPath("$.series[0].geometry.couplingFace").value(20.0))
                .andReturn().getResponse().getContentAsString();
        Object seriesObj = JsonPath.read(snap, "$.series[0].id");
        String series = String.valueOf(seriesObj);
        List<Number> antIds = JsonPath.read(snap, "$.colors[?(@.code=='ANT-1')].id");
        String antId = String.valueOf(antIds.get(0));

        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Renk\"}"), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"P\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}"),
                "$.id");

        String bomWithColor = TestData.BOM.trim();
        bomWithColor = bomWithColor.substring(0, bomWithColor.length() - 1)
                + ",{\"group\":\"COLOR\",\"colorCode\":\"ANT-1\",\"qty\":1}]";

        String beforeBody = call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"P\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + bomWithColor + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        double before = ((Number) JsonPath.read(beforeBody, "$.unitPrice")).doubleValue();

        call(put("/api/v1/catalog/colors/" + antId), token,
                "{\"code\":\"ANT-1\",\"name\":\"Antrasit (tek yüz)\",\"hexOutside\":\"#3B3F45\","
                        + "\"hexInside\":\"#F4F5F2\",\"surchargePct\":30,\"isActive\":true,\"sortOrder\":30}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surchargePct").value(30));

        call(get("/api/v1/windows/" + window + "/quote"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.not(before)));
    }
}
