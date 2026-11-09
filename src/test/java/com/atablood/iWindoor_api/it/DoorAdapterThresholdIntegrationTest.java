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

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
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
class DoorAdapterThresholdIntegrationTest {

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

    @Test
    void newCompanyGetsDoorProfilesAndHardwareRules() throws Exception {
        String token = register();

        mvc.perform(get("/api/v1/catalog/snapshot").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].geometry.doorSashFace").value(110.0))
                .andExpect(jsonPath("$.series[0].geometry.thresholdFace").value(20.0))
                .andExpect(jsonPath("$.profiles[*].type", hasItem("DOOR_SASH")))
                .andExpect(jsonPath("$.profiles[*].type", hasItem("THRESHOLD")))
                .andExpect(jsonPath("$.profiles[*].type", hasItem("ADAPTER")));

        mvc.perform(get("/api/v1/catalog/hardware-rules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].kind", hasItem("DOOR")))
                .andExpect(jsonPath("$[*].kind", hasItem("PASSIVE")))
                .andExpect(jsonPath("$[*].kind", hasItem("DOOR_PASSIVE")))
                .andExpect(jsonPath("$[*].accessoryCode", hasItem("KAPI-KILIT-SET")));
    }
}
