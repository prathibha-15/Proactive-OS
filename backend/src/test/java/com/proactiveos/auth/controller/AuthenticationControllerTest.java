package com.proactiveos.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import java.time.Instant;

import com.proactiveos.auth.dto.LoginResponse;
import com.proactiveos.auth.dto.UserResponse;
import com.proactiveos.auth.config.SecurityConfiguration;
import com.proactiveos.auth.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

@WebMvcTest({AuthenticationController.class, com.proactiveos.health.HealthController.class})
@Import(SecurityConfiguration.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationIsPublicAndReturnsNoPasswordHash() throws Exception {
        when(authenticationService.register(any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginIsPublicAndReturnsBearerTokenAndSafeUser() throws Exception {
        when(authenticationService.login(any())).thenReturn(
                new LoginResponse("signed.jwt.token", "Bearer", 3600L, userResponse()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void rejectsInvalidRegistrationInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthCheckRemainsPublic() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(cookie().doesNotExist("JSESSIONID"));
        }

        @Test
        void invalidCredentialsReturnUnauthorized() throws Exception {
        when(authenticationService.login(any())).thenThrow(
            new com.proactiveos.auth.service.InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"wrong-pass\"}"))
            .andExpect(status().isUnauthorized());
    }

    private UserResponse userResponse() {
        Instant now = Instant.parse("2026-10-02T10:00:00Z");
        return new UserResponse(42L, "user@example.com", now, now);
    }
}