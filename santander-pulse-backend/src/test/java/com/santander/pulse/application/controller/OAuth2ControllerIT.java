package com.santander.pulse.application.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2ControllerIT {

    private static final String CONTEXT_PATH = "/api";
    private static final String LOGIN_ENDPOINT = "/login";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should expose providers metadata when info flag is true")
    void shouldExposeProvidersMetadataWhenInfoFlagIsTrue() throws Exception {
        mockMvc.perform(get(CONTEXT_PATH + LOGIN_ENDPOINT)
                        .contextPath(CONTEXT_PATH)
                        .servletPath(LOGIN_ENDPOINT)
                        .param("info", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Utilize POST /auth/login para CPF/senha ou acesse /oauth2/authorization/google para login com Google."))
                .andExpect(jsonPath("$.providers.cpf.type").value("password"))
                .andExpect(jsonPath("$.providers.cpf.endpoint").value("/auth/login"))
                .andExpect(jsonPath("$.providers.google.type").value("oauth2"))
                .andExpect(jsonPath("$.providers.google.endpoint").value("/oauth2/authorization/google"));
    }
}