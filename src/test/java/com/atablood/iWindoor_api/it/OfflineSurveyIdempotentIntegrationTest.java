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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class OfflineSurveyIdempotentIntegrationTest {

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
    void clientIdIdempotentCreatePhotoAndDelete() throws Exception {
        String token = register();
        String project = str(call(post("/api/v1/projects"), token, "{\"name\":\"Offline\"}")
                .andExpect(status().isOk()), "$.id");

        String body = "{\"room\":\"Salon\",\"productType\":\"WINDOW\",\"holeWidth\":1420,\"holeHeight\":1420,"
                + "\"quantity\":1,\"clientId\":\"offline-item-1\"}";
        String id1 = str(call(post("/api/v1/projects/" + project + "/survey"), token, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value("offline-item-1")), "$.id");

        String id2 = str(call(post("/api/v1/projects/" + project + "/survey"), token, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id1))), "$.id");
        org.junit.jupiter.api.Assertions.assertEquals(id1, id2);

        call(get("/api/v1/projects/" + project + "/survey"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8,
                (byte) 0xCF, (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x05, (byte) 0xFE, (byte) 0x02,
                (byte) 0xFE, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        MockMultipartFile photo = new MockMultipartFile("file", "a.png", "image/png", png);
        String photoId1 = str(mvc.perform(multipart("/api/v1/projects/" + project + "/survey/" + id1 + "/photos")
                        .file(photo).param("clientId", "offline-photo-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()), "$.id");

        String photoId2 = str(mvc.perform(multipart("/api/v1/projects/" + project + "/survey/" + id1 + "/photos")
                        .file(photo).param("clientId", "offline-photo-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()), "$.id");
        org.junit.jupiter.api.Assertions.assertEquals(photoId1, photoId2);

        call(get("/api/v1/projects/" + project + "/survey"), token, null)
                .andExpect(jsonPath("$[0].photoIds.length()").value(1));

        call(delete("/api/v1/projects/" + project + "/survey/" + id1), token, null)
                .andExpect(status().isNoContent());
        call(delete("/api/v1/projects/" + project + "/survey/" + id1), token, null)
                .andExpect(status().isNoContent());
    }
}
