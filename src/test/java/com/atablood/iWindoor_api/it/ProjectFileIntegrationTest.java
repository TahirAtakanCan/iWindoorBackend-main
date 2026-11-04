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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class ProjectFileIntegrationTest {

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
    void exportImportRoundTripAndInvalidVersion() throws Exception {
        String token = register();
        String series = str(call(get("/api/v1/catalog/snapshot"), token, null).andExpect(status().isOk()), "$.series[0].id");
        String seriesName = str(call(get("/api/v1/catalog/snapshot"), token, null), "$.series[0].name");
        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Export Projesi\"}").andExpect(status().isOk()), "$.id");
        String design = TestData.DESIGN.formatted(series);
        String window = str(call(post("/api/v1/projects/" + project + "/windows"), token,
                "{\"name\":\"Salon\",\"width\":1800,\"height\":1700,\"seriesId\":" + series + ",\"design\":" + design + "}")
                .andExpect(status().isOk()), "$.id");
        call(put("/api/v1/windows/" + window), token,
                "{\"name\":\"Salon\",\"quantity\":2,\"design\":" + design + ",\"bom\":" + TestData.BOM + ",\"rowVersion\":0}")
                .andExpect(jsonPath("$.unitPrice").value(4816.31));

        ResultActions exported = call(get("/api/v1/projects/" + project + "/export"), token, null)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"export-projesi.iwd\""))
                .andExpect(jsonPath("$.format").value("iwindoor-project"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.windows.length()").value(1));
        String exportJson = exported.andReturn().getResponse().getContentAsString();

        ResultActions imported = call(post("/api/v1/projects/import"), token, exportJson)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Export Projesi (içe aktarıldı)"))
                .andExpect(jsonPath("$.windowUnits.length()").value(1))
                .andExpect(jsonPath("$.windowUnits[0].unitPrice").value(4816.31));

        String importedWindowId = str(imported, "$.windowUnits[0].id");
        call(get("/api/v1/windows/" + importedWindowId), token, null)
                .andExpect(jsonPath("$.design.root.type").value("split"))
                .andExpect(jsonPath("$.unitPrice").value(4816.31));

        // seriesName eşleşmesi için export gövdesinde adın durduğunu doğrula
        call(get("/api/v1/projects/" + project + "/export"), token, null)
                .andExpect(jsonPath("$.windows[0].seriesName").value(seriesName));

        String bad = exportJson.replace("\"version\":1", "\"version\":99");
        call(post("/api/v1/projects/import"), token, bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROJECT_FILE"));
    }
}
