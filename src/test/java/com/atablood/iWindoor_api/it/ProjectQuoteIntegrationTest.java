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
class ProjectQuoteIntegrationTest {

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

    private String projectWithPricedWindow(String token) throws Exception {
        String series = str(call(get("/api/v1/catalog/snapshot"), token, null).andExpect(status().isOk()), "$.series[0].id");
        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Teklif Projesi\"}").andExpect(status().isOk()), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");
        call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"Salon\",\"quantity\":2,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(4816.31));
        return project;
    }

    @Test
    void quoteTotalsAndAccessControl() throws Exception {
        String t1 = register(), t2 = register();
        String project = projectWithPricedWindow(t1);

        // Malzeme 4816.31 → COST_PLUS satış 8846.90 × 2 = 17693.80
        call(get("/api/v1/projects/" + project + "/quote"), t1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(17693.80))
                .andExpect(jsonPath("$.discountPct").value(0))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.netTotal").value(17693.80))
                .andExpect(jsonPath("$.vatRate").value(20))
                .andExpect(jsonPath("$.vatAmount").value(3538.76))
                .andExpect(jsonPath("$.grandTotal").value(21232.56))
                .andExpect(jsonPath("$.windows[0].materialUnitPrice").value(4816.31))
                .andExpect(jsonPath("$.windows[0].saleUnitPrice").value(8846.90))
                .andExpect(jsonPath("$.windows[0].lineTotal").value(17693.80))
                .andExpect(jsonPath("$.materials.length()", greaterThan(0)));

        call(get("/api/v1/projects/" + project + "/quote").param("discountPct", "10"), t1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(17693.80))
                .andExpect(jsonPath("$.discountPct").value(10))
                .andExpect(jsonPath("$.discountAmount").value(1769.38))
                .andExpect(jsonPath("$.netTotal").value(15924.42))
                .andExpect(jsonPath("$.vatAmount").value(3184.88))
                .andExpect(jsonPath("$.grandTotal").value(19109.30));

        call(get("/api/v1/projects/" + project + "/quote"), t2, null).andExpect(status().isForbidden());
    }
}
