package com.atablood.iWindoor_api.it;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class VersionsAndSurveyIntegrationTest {

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
        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Sürüm Projesi\"}")
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
    void versionsRestoreAndSurveyPhotos() throws Exception {
        String token = register();
        String project = projectWithWindow(token);

        java.math.BigDecimal grand = new java.math.BigDecimal(str(
                call(get("/api/v1/projects/" + project + "/quote"), token, null).andExpect(status().isOk()),
                "$.grandTotal"));

        String v1 = str(call(post("/api/v1/projects/" + project + "/versions"), token, "{\"label\":\"İlk\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.grandTotal").value(grand.doubleValue())), "$.id");

        call(get("/api/v1/projects/" + project + "/versions"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String windowId = str(call(get("/api/v1/projects/" + project), token, null), "$.windowUnits[0].id");
        call(delete("/api/v1/windows/" + windowId), token, null).andExpect(status().isNoContent());

        call(post("/api/v1/projects/" + project + "/versions"), token, "{\"label\":\"Boş\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNo").value(2));

        call(post("/api/v1/projects/" + project + "/versions/" + v1 + "/restore"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.windowUnits.length()").value(1));

        call(get("/api/v1/projects/" + project + "/versions"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.label=='Geri yüklemeden önce (otomatik)')]").isNotEmpty());

        // APPROVED → otomatik sürüm
        call(post("/api/v1/projects/" + project + "/status"), token, "{\"status\":\"APPROVED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedVersionId").isNumber());

        String approvedVersionId = str(call(get("/api/v1/projects/" + project), token, null), "$.approvedVersionId");

        call(post("/api/v1/projects/" + project + "/versions/" + v1 + "/restore"), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_LOCKED"));

        call(delete("/api/v1/projects/" + project + "/versions/" + approvedVersionId), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_APPROVED"));

        // Keşif
        String surveyId = str(call(post("/api/v1/projects/" + project + "/survey"), token,
                "{\"room\":\"Salon\",\"productType\":\"WINDOW\",\"holeWidth\":1420,\"holeHeight\":1420,\"quantity\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedWidth").value(1400))
                .andExpect(jsonPath("$.suggestedHeight").value(1400)), "$.id");

        // 1x1 PNG
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8,
                (byte) 0xCF, (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x05, (byte) 0xFE, (byte) 0x02,
                (byte) 0xFE, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        MockMultipartFile photo = new MockMultipartFile("file", "a.png", "image/png", png);
        String photoId = str(mvc.perform(multipart("/api/v1/projects/" + project + "/survey/" + surveyId + "/photos")
                        .file(photo).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()), "$.id");

        byte[] downloaded = mvc.perform(get("/api/v1/survey-photos/" + photoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertArrayEquals(png, downloaded);

        MockMultipartFile tooBig = new MockMultipartFile("file", "big.png", "image/png", new byte[4 * 1024 * 1024]);
        mvc.perform(multipart("/api/v1/projects/" + project + "/survey/" + surveyId + "/photos")
                        .file(tooBig).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        MockMultipartFile plain = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        mvc.perform(multipart("/api/v1/projects/" + project + "/survey/" + surveyId + "/photos")
                        .file(plain).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
