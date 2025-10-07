package com.santander.pulse.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.domain.User;
import com.santander.pulse.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    private static final String CONTEXT_PATH = "/api";
    private static final String AUTH_LOGIN_ENDPOINT = "/auth/login";
    private static final String AUTH_PROVIDERS_ENDPOINT = "/auth/providers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User seededUser = new User("12345678901", "user@santander.com", passwordEncoder.encode("password123"), "Usuário Teste");
        seededUser.setCpf("12345678901");
        userRepository.save(seededUser);
    }

    @Nested
    @DisplayName("CPF/Senha authentication")
    class CpfPasswordAuthentication {

        @Test
        @DisplayName("should issue JWT tokens when credentials are valid")
        void shouldReturnTokensWhenCredentialsAreValid() throws Exception {
            String payload = "{" +
                    "\"cpf\":\"12345678901\"," +
                    "\"password\":\"password123\"" +
                    "}";

    ResultActions result = mockMvc.perform(post(CONTEXT_PATH + AUTH_LOGIN_ENDPOINT)
            .contextPath(CONTEXT_PATH)
            .servletPath(AUTH_LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.username").value("12345678901"));

            String responseBody = result.andReturn().getResponse().getContentAsString();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            assertThat(responseJson.get("tokenType").asText()).isEqualTo("Bearer");
            assertThat(responseJson.get("expiresIn").asLong()).isGreaterThan(0L);
        }

        @Test
        @DisplayName("should reject invalid credentials")
        void shouldRejectInvalidCredentials() throws Exception {
            String payload = "{" +
                    "\"cpf\":\"12345678901\"," +
                    "\"password\":\"wrongpass\"" +
                    "}";

    mockMvc.perform(post(CONTEXT_PATH + AUTH_LOGIN_ENDPOINT)
                .contextPath(CONTEXT_PATH)
                .servletPath(AUTH_LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("should advertise Google OAuth provider when client is configured")
    void shouldExposeGoogleProvider() throws Exception {
        mockMvc.perform(get(CONTEXT_PATH + AUTH_PROVIDERS_ENDPOINT)
                .contextPath(CONTEXT_PATH)
                .servletPath(AUTH_PROVIDERS_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.providers.google.enabled").value(true))
            .andExpect(jsonPath("$.providers.google.authorizationUrl").value("http://localhost/api/login?provider=google&redirect_uri=http://localhost:4200/oauth2/callback"))
            .andExpect(jsonPath("$.providers.google.authorizationUrl").value(containsString("provider=google")))
            .andExpect(jsonPath("$.providers.google.redirectUri").value("http://localhost/api/login/oauth2/code/google"))
            .andExpect(jsonPath("$.providers.google.postLoginRedirect").value("http://localhost:4200/oauth2/callback"))
            .andExpect(jsonPath("$.providers.google.scopes").isArray());
    }
}
