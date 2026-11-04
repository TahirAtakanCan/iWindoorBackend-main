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
class CustomerAndSashLimitsIntegrationTest {

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
    void customerFieldsPersisted_legacyNameOnlyStillWorks_quoteAndExportImport() throws Exception {
        String token = register();

        call(get("/api/v1/catalog/snapshot"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].geometry.maxSashWidth").value(1400.0))
                .andExpect(jsonPath("$.series[0].geometry.minSashWidth").value(350.0))
                .andExpect(jsonPath("$.series[0].geometry.maxFixedGlassArea").value(6.0));

        // Eski mobil: sadece name/description
        call(post("/api/v1/projects"), token, "{\"name\":\"Eski İstek\",\"description\":\"d\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Eski İstek"))
                .andExpect(jsonPath("$.customerName").value(org.hamcrest.Matchers.nullValue()));

        String series = str(call(get("/api/v1/catalog/snapshot"), token, null), "$.series[0].id");
        String createBody = """
                {"name":"Şişli Ofis","description":"İş","customerName":"Ayşe Yılmaz",
                 "customerPhone":"05321234567","customerEmail":"ayse@example.com",
                 "customerAddress":"Şişli / İstanbul"}""";
        String project = str(call(post("/api/v1/projects"), token, createBody).andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.customerPhone").value("05321234567"))
                .andExpect(jsonPath("$.customerEmail").value("ayse@example.com"))
                .andExpect(jsonPath("$.customerAddress").value("Şişli / İstanbul")), "$.id");

        call(get("/api/v1/projects/" + project), token, null)
                .andExpect(jsonPath("$.customerName").value("Ayşe Yılmaz"));

        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");
        call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"Salon\",\"quantity\":2,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(status().isOk());

        call(get("/api/v1/projects/" + project + "/quote"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.customerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.project.customerEmail").value("ayse@example.com"));

        ResultActions exported = call(get("/api/v1/projects/" + project + "/export"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.customerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.project.customerAddress").value("Şişli / İstanbul"));
        // Türkçe slug dosya adı
        String disposition = exported.andReturn().getResponse().getHeader("Content-Disposition");
        org.junit.jupiter.api.Assertions.assertTrue(disposition != null && disposition.contains("sisli-ofis.iwd"));

        String exportJson = exported.andReturn().getResponse().getContentAsString();
        call(post("/api/v1/projects/import"), token, exportJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.customerPhone").value("05321234567"))
                .andExpect(jsonPath("$.customerEmail").value("ayse@example.com"))
                .andExpect(jsonPath("$.customerAddress").value("Şişli / İstanbul"));
    }
}
