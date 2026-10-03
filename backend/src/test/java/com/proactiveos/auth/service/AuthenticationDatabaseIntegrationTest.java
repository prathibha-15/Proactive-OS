package com.proactiveos.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.proactiveos.auth.dto.LoginRequest;
import com.proactiveos.auth.dto.RegisterRequest;
import com.proactiveos.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthenticationDatabaseIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void persistsNormalizedAccountWithBcryptAndAuthenticatesWithJwt() {
        String email = "phase7-" + UUID.randomUUID() + "@example.com";
        var registration = authenticationService.register(new RegisterRequest(email.toUpperCase(), "safe-pass-123"));

        var storedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(storedUser.getEmail()).isEqualTo(email);
        assertThat(storedUser.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("safe-pass-123", storedUser.getPasswordHash())).isTrue();
        assertThat(registration.toString()).doesNotContain("passwordHash", "safe-pass-123");

        var login = authenticationService.login(new LoginRequest(email.toUpperCase(), "safe-pass-123"));
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.user().id()).isEqualTo(registration.id());
        assertThat(login.expiresIn()).isEqualTo(3600L);

        assertThatThrownBy(() -> authenticationService.register(new RegisterRequest(email, "safe-pass-123")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}