package com.atablood.iWindoor_api.it;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class SlidingAndScreensIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void newCompanyHasScreenAccessoryWithM2() throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String body = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Admin\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        String accessories = mvc.perform(get("/api/v1/catalog/accessories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> units = JsonPath.read(accessories, "$[?(@.code=='SINEKLIK-PLISE')].unit");
        org.junit.jupiter.api.Assertions.assertFalse(units.isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals("M2", units.get(0));

        mvc.perform(get("/api/v1/company").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.screenMinAreaM2").value(0.50));
    }
}
