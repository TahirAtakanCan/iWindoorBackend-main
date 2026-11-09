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

import static org.hamcrest.Matchers.*;
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
class WorkflowIntegrationTest {

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

    private String projectWithWindow(String token) throws Exception {
        String series = str(call(get("/api/v1/catalog/snapshot"), token, null).andExpect(status().isOk()), "$.series[0].id");
        String project = str(call(post("/api/v1/projects"), token,
                "{\"name\":\"İş Takip\",\"customerName\":\"Ali Veli\",\"customerPhone\":\"05551112233\"}")
                .andExpect(status().isOk()), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");
        call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"Salon\",\"quantity\":1,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(status().isOk());
        return project;
    }

    @Test
    void statusPaymentsPermissionsCustomerDashboard() throws Exception {
        String adminToken = register();
        String project = projectWithWindow(adminToken);

        // Teklif genel toplamı (COST_PLUS satış + KDV)
        java.math.BigDecimal grand = new java.math.BigDecimal(str(
                call(get("/api/v1/projects/" + project + "/quote"), adminToken, null).andExpect(status().isOk()),
                "$.grandTotal")).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal expectedBalance = grand.subtract(new java.math.BigDecimal("15000.00"));

        call(post("/api/v1/projects/" + project + "/status"), adminToken,
                "{\"status\":\"APPROVED\",\"note\":\"Onay\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedTotal").value(grand.doubleValue()));

        call(post("/api/v1/projects/" + project + "/payments"), adminToken,
                "{\"kind\":\"PAYMENT\",\"amount\":15000,\"paidAt\":\"2026-11-01\",\"method\":\"CASH\",\"note\":\"Peşinat\"}")
                .andExpect(status().isOk());

        call(get("/api/v1/projects/" + project + "/payments"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(15000))
                .andExpect(jsonPath("$.target").value(grand.doubleValue()))
                .andExpect(jsonPath("$.balance").value(expectedBalance.doubleValue()));

        call(post("/api/v1/projects/" + project + "/payments"), adminToken,
                "{\"kind\":\"REFUND\",\"amount\":1000,\"paidAt\":\"2026-11-02\",\"method\":\"CASH\"}")
                .andExpect(status().isOk());

        call(get("/api/v1/projects/" + project + "/payments"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(14000));

        call(post("/api/v1/projects/" + project + "/payments"), adminToken,
                "{\"kind\":\"PAYMENT\",\"amount\":0,\"paidAt\":\"2026-11-03\",\"method\":\"CASH\"}")
                .andExpect(status().isBadRequest());

        // Geçersiz geçiş: yeni proje QUOTE→PRODUCTION
        String p2 = str(call(post("/api/v1/projects"), adminToken, "{\"name\":\"Diğer\"}").andExpect(status().isOk()), "$.id");
        call(post("/api/v1/projects/" + p2 + "/status"), adminToken, "{\"status\":\"PRODUCTION\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        String empEmail = UUID.randomUUID().toString().substring(0, 8) + "@emp.test";
        call(post("/api/v1/users"), adminToken,
                "{\"fullName\":\"Çalışan\",\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}")
                .andExpect(status().isOk());
        String empBody = mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String empToken = JsonPath.read(empBody, "$.token");

        call(post("/api/v1/projects/" + project + "/status"), empToken, "{\"status\":\"QUOTE\"}")
                .andExpect(status().isForbidden());

        String paymentId = str(call(get("/api/v1/projects/" + project + "/payments"), adminToken, null), "$.payments[0].id");
        call(delete("/api/v1/projects/" + project + "/payments/" + paymentId), empToken, null)
                .andExpect(status().isForbidden());

        // Onay sonrası fiyat değişimi
        String series = str(call(get("/api/v1/catalog/snapshot"), adminToken, null), "$.series[0].id");
        String design = TestData.DESIGN.formatted(series);
        String windowId = str(call(get("/api/v1/projects/" + project), adminToken, null), "$.windowUnits[0].id");
        int rv = ((Number) JsonPath.read(
                call(get("/api/v1/windows/" + windowId), adminToken, null).andReturn().getResponse().getContentAsString(),
                "$.rowVersion")).intValue();
        call(put("/api/v1/windows/" + windowId), adminToken,
                "{\"name\":\"Salon\",\"quantity\":2,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":" + rv + "}")
                .andExpect(status().isOk());
        call(get("/api/v1/projects/" + project), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceChangedAfterApproval").value(true))
                .andExpect(jsonPath("$.priceDelta", notNullValue()));

        // Müşteri silme — projesi var
        String customerId = str(call(get("/api/v1/projects/" + project), adminToken, null), "$.customerId");
        call(delete("/api/v1/customers/" + customerId), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_IN_USE"));

        // customer_name ile oluşturulmuş proje müşteriye bağlı
        call(get("/api/v1/customers/" + customerId), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ali Veli"))
                .andExpect(jsonPath("$.projects[?(@.id==" + project + ")]").isNotEmpty());

        String dash = call(get("/api/v1/dashboard"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivables").isNumber())
                .andExpect(jsonPath("$.approvedThisMonth").isNumber())
                .andReturn().getResponse().getContentAsString();
        int approvedCount = ((Number) JsonPath.read(dash, "$.countsByStatus.APPROVED")).intValue();
        double approvedMonth = ((Number) JsonPath.read(dash, "$.approvedThisMonth")).doubleValue();
        org.junit.jupiter.api.Assertions.assertTrue(approvedCount >= 1);
        org.junit.jupiter.api.Assertions.assertTrue(approvedMonth >= grand.doubleValue() - 0.01);
    }
}
