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
import java.util.Map;
import java.util.UUID;

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
class DesignTemplatesIntegrationTest {

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

    @Test
    void createListDuplicateInvalidAndCrossCompanyRoles() throws Exception {
        String admin = register();
        String other = register();
        String name = "Sablon-" + UUID.randomUUID().toString().substring(0, 6);
        String design = TestData.DESIGN.formatted("12");

        String created = call(post("/api/v1/templates"), admin,
                "{\"name\":\"" + name + "\",\"category\":\"WINDOW\",\"design\":" + design + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.category").value("WINDOW"))
                .andExpect(jsonPath("$.design.schemaVersion").value(1))
                .andExpect(jsonPath("$.design.width").value(1800))
                .andExpect(jsonPath("$.design.root.type").value("split"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        call(get("/api/v1/templates"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)))
                .andExpect(jsonPath("$[?(@.name=='" + name + "')].design.width", hasItem(1800)));

        call(post("/api/v1/templates"), admin,
                "{\"name\":\"" + name.toLowerCase() + "\",\"category\":\"DOOR\",\"design\":" + design + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEMPLATE_NAME_TAKEN"));

        String broken = """
                {"schemaVersion":1,"width":1800,"height":1700,"color":"white",
                 "root":{"id":"n1","type":"split","dir":"H","sizes":[400,1300],
                 "children":[{"id":"n2","type":"glass","glass":"4+16+4"}]}}""";
        call(post("/api/v1/templates"), admin,
                "{\"name\":\"Bozuk-" + UUID.randomUUID().toString().substring(0, 6)
                        + "\",\"category\":\"WINDOW\",\"design\":" + broken + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DESIGN"));

        call(get("/api/v1/templates"), other, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.not(hasItem(name))));
        call(put("/api/v1/templates/" + id), other,
                "{\"name\":\"Hacker\"}")
                .andExpect(status().isNotFound());
        call(delete("/api/v1/templates/" + id), other, null)
                .andExpect(status().isNotFound());

        String empEmail = UUID.randomUUID().toString().substring(0, 8) + "@emp.test";
        call(post("/api/v1/users"), admin,
                "{\"fullName\":\"Emp\",\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}")
                .andExpect(status().isOk());
        String empToken = JsonPath.read(mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + empEmail + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");

        call(post("/api/v1/templates"), empToken,
                "{\"name\":\"Emp-" + UUID.randomUUID().toString().substring(0, 6)
                        + "\",\"category\":\"WINDOW\",\"design\":" + design + "}")
                .andExpect(status().isForbidden());
        call(get("/api/v1/templates"), empToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));

        // design alanının GET yanıtında aynen durduğunu doğrula
        String listJson = call(get("/api/v1/templates"), admin, null)
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> designs = JsonPath.read(listJson, "$[?(@.name=='" + name + "')].design");
        org.junit.jupiter.api.Assertions.assertFalse(designs.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(1, ((Number) designs.get(0).get("schemaVersion")).intValue());
        org.junit.jupiter.api.Assertions.assertEquals(1800, ((Number) designs.get(0).get("width")).intValue());
    }
}
