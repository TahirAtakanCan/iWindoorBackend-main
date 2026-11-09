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

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class CompanyApiIntegrationTest {

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
                        .content("{\"fullName\":\"Firma Admin\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String token,
                               String json) throws Exception {
        b.header("Authorization", "Bearer " + token);
        if (json != null) b.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(b);
    }

    private static final String BASE_UPDATE = """
            {"name":"Yeni Firma A.Ş.","address":"Ankara","taxNumber":"123","taxOffice":"Çankaya",
             "phone":"03121234567","email":"info@firma.test","vatRate":18,"defaultDiscountPct":5,
             "currency":"TRY","quoteNote":"Teşekkürler","quoteValidityDays":30,
             "sawKerfMm":4,"barTrimMm":10,"minOffcutMm":500,
             "glassSupplierName":null,"glassSupplierPhone":null,"glassSupplierEmail":null,"glassOrderNote":null,
             "pricingMode":"COST_PLUS","profitMarginPct":30,"laborPerM2":400,"installPerM2":250,"minBillableAreaM2":1.00,
             "surveyGapMm":10}""";

    @Test
    void companyAdminCanUpdateDefaults_employeeForbidden() throws Exception {
        String adminToken = register();

        call(get("/api/v1/company"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vatRate").value(20))
                .andExpect(jsonPath("$.defaultDiscountPct").value(0))
                .andExpect(jsonPath("$.currency").value("TRY"))
                .andExpect(jsonPath("$.quoteValidityDays").value(15))
                .andExpect(jsonPath("$.sawKerfMm").value(4))
                .andExpect(jsonPath("$.barTrimMm").value(10))
                .andExpect(jsonPath("$.minOffcutMm").value(500))
                .andExpect(jsonPath("$.pricingMode").value("COST_PLUS"))
                .andExpect(jsonPath("$.profitMarginPct").value(30));

        call(put("/api/v1/company"), adminToken, BASE_UPDATE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Firma A.Ş."))
                .andExpect(jsonPath("$.vatRate").value(18))
                .andExpect(jsonPath("$.defaultDiscountPct").value(5))
                .andExpect(jsonPath("$.quoteValidityDays").value(30));

        String empEmail = UUID.randomUUID().toString().substring(0, 8) + "@emp.test";
        call(post("/api/v1/users"), adminToken,
                "{\"fullName\":\"Çalışan\",\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}")
                .andExpect(status().isOk());

        String empBody = mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String empToken = JsonPath.read(empBody, "$.token");

        call(put("/api/v1/company"), empToken, BASE_UPDATE).andExpect(status().isForbidden());

        call(get("/api/v1/company"), empToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vatRate").value(18));
    }

    @Test
    void productionSettingsDefaultsAndValidation() throws Exception {
        String token = register();

        call(get("/api/v1/company"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sawKerfMm").value(4))
                .andExpect(jsonPath("$.barTrimMm").value(10))
                .andExpect(jsonPath("$.minOffcutMm").value(500));

        String badKerf = """
                {"name":"Firma","address":null,"taxNumber":null,"taxOffice":null,"phone":null,"email":null,
                 "vatRate":20,"defaultDiscountPct":0,"currency":"TRY","quoteNote":null,"quoteValidityDays":15,
                 "sawKerfMm":25,"barTrimMm":10,"minOffcutMm":500,
                 "glassSupplierName":null,"glassSupplierPhone":null,"glassSupplierEmail":null,"glassOrderNote":null,
                 "pricingMode":"COST_PLUS","profitMarginPct":30,"laborPerM2":400,"installPerM2":250,"minBillableAreaM2":1.00,
                 "surveyGapMm":10}""";
        call(put("/api/v1/company"), token, badKerf).andExpect(status().isBadRequest());

        String ok = """
                {"name":"Firma","address":null,"taxNumber":null,"taxOffice":null,"phone":null,"email":null,
                 "vatRate":20,"defaultDiscountPct":0,"currency":"TRY","quoteNote":null,"quoteValidityDays":15,
                 "sawKerfMm":3.5,"barTrimMm":12,"minOffcutMm":800,
                 "glassSupplierName":"Cam A.Ş.","glassSupplierPhone":"0555","glassSupplierEmail":"cam@test.com",
                 "glassOrderNote":"Isıcam çıtası gri",
                 "pricingMode":"COST_PLUS","profitMarginPct":30,"laborPerM2":400,"installPerM2":250,"minBillableAreaM2":1.00,
                 "surveyGapMm":12}""";
        call(put("/api/v1/company"), token, ok)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sawKerfMm").value(3.5))
                .andExpect(jsonPath("$.barTrimMm").value(12))
                .andExpect(jsonPath("$.minOffcutMm").value(800))
                .andExpect(jsonPath("$.glassSupplierName").value("Cam A.Ş."))
                .andExpect(jsonPath("$.glassOrderNote").value("Isıcam çıtası gri"));

        call(get("/api/v1/company"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sawKerfMm").value(3.5))
                .andExpect(jsonPath("$.barTrimMm").value(12))
                .andExpect(jsonPath("$.minOffcutMm").value(800))
                .andExpect(jsonPath("$.glassSupplierEmail").value("cam@test.com"));
    }
}
