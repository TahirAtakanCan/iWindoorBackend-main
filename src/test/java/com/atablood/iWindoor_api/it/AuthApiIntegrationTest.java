package com.atablood.iWindoor_api.it;

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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth kayıt/giriş uçtan uca. PostgreSQL ister.
 * Çalıştırma: IWINDOOR_IT=true ./mvnw test
 */
@EnabledIfEnvironmentVariable(named = "IWINDOOR_IT", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=${IWINDOOR_TEST_DB_URL:jdbc:postgresql://localhost:5432/windoor_test}",
        "spring.datasource.username=${IWINDOOR_TEST_DB_USER:admin}",
        "spring.datasource.password=${IWINDOOR_TEST_DB_PASSWORD:123456789}"
})
class AuthApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static String randomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    @Test
    void registerSuccessAndCatalogSnapshotNonEmpty() throws Exception {
        String email = randomEmail();
        String body = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test Admin\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role").value("COMPANY_ADMIN"))
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");
        mvc.perform(get("/api/v1/catalog/snapshot").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series.length()", greaterThan(0)));
    }

    @Test
    void registerWithCompanyName() throws Exception {
        String email = randomEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ali Veli\",\"email\":\"" + email
                                + "\",\"password\":\"secret123\",\"companyName\":\"Veli PVC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.role").value("COMPANY_ADMIN"));
    }

    @Test
    void duplicateEmailDifferentCaseReturns409() throws Exception {
        String email = randomEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());

        String upper = email.toUpperCase();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"B\",\"email\":\"" + upper + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void invalidEmailReturns400ValidationFailed() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
    }

    @Test
    void shortPasswordReturns400ValidationFailed() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + randomEmail() + "\",\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")));
    }

    @Test
    void loginWithUppercaseEmailAfterRegister() throws Exception {
        String email = randomEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = randomEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized());
    }
}
