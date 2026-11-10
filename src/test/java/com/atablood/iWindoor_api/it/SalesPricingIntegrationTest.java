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

import static org.hamcrest.Matchers.nullValue;
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
class SalesPricingIntegrationTest {

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

    private static final String COMPANY_BODY = """
            {"name":"Satış Firma","address":null,"taxNumber":null,"taxOffice":null,"phone":null,"email":null,
             "vatRate":20,"defaultDiscountPct":0,"currency":"TRY","quoteNote":null,"quoteValidityDays":15,
             "sawKerfMm":4,"barTrimMm":10,"minOffcutMm":500,
             "glassSupplierName":null,"glassSupplierPhone":null,"glassSupplierEmail":null,"glassOrderNote":null,
             "pricingMode":"COST_PLUS","profitMarginPct":%s,"laborPerM2":400,"installPerM2":250,"minBillableAreaM2":1.00,
             "surveyGapMm":10,"screenMinAreaM2":0.50}""";

    @Test
    void extrasMarginAndEmployeeCostSummary() throws Exception {
        String adminToken = register();
        String series = str(call(get("/api/v1/catalog/snapshot"), adminToken, null).andExpect(status().isOk()), "$.series[0].id");
        String project = str(call(post("/api/v1/projects"), adminToken, "{\"name\":\"Satış\"}").andExpect(status().isOk()), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), adminToken,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");
        call(put("/api/v1/windows/" + window), adminToken,
                "{\"name\":\"Salon\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(4816.31))
                .andExpect(jsonPath("$.saleUnitPrice").value(8846.90));

        call(post("/api/v1/projects/" + project + "/extras"), adminToken,
                "{\"name\":\"Nakliye\",\"quantity\":1,\"unit\":\"adet\",\"unitPrice\":1500,\"sortOrder\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nakliye"))
                .andExpect(jsonPath("$.lineTotal").value(1500));

        // 8846.90 + 1500 = 10346.90
        call(get("/api/v1/projects/" + project + "/quote"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(10346.90))
                .andExpect(jsonPath("$.extras.length()").value(1))
                .andExpect(jsonPath("$.costSummary.saleSubtotal").value(10346.90))
                .andExpect(jsonPath("$.windows[0].materialUnitPrice").value(4816.31))
                .andExpect(jsonPath("$.windows[0].saleUnitPrice").value(8846.90));

        double materialBefore = ((Number) JsonPath.read(
                call(get("/api/v1/windows/" + window), adminToken, null).andReturn().getResponse().getContentAsString(),
                "$.unitPrice")).doubleValue();
        double saleBefore = ((Number) JsonPath.read(
                call(get("/api/v1/windows/" + window), adminToken, null).andReturn().getResponse().getContentAsString(),
                "$.saleUnitPrice")).doubleValue();

        call(put("/api/v1/company"), adminToken, COMPANY_BODY.formatted("40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profitMarginPct").value(40));

        call(get("/api/v1/windows/" + window), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value(materialBefore))
                .andExpect(jsonPath("$.saleUnitPrice", org.hamcrest.Matchers.greaterThan(saleBefore)));

        String empEmail = UUID.randomUUID().toString().substring(0, 8) + "@emp.test";
        call(post("/api/v1/users"), adminToken,
                "{\"fullName\":\"Çalışan\",\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}")
                .andExpect(status().isOk());
        String empBody = mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String empToken = JsonPath.read(empBody, "$.token");

        call(get("/api/v1/projects/" + project + "/quote"), empToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costSummary", nullValue()));
    }
}
