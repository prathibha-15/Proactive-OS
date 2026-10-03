package com.proactiveos.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.proactiveos.auth.dto.LoginRequest;
import com.proactiveos.auth.dto.RegisterRequest;
import com.proactiveos.auth.entity.User;
import com.proactiveos.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void registersNormalizedEmailAndStoresOnlyEncodedPassword() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encoded-value");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authenticationService.register(new RegisterRequest(" User@Example.com ", "password123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded-value")
                .isNotEqualTo("password123");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.toString()).doesNotContain("password123", "passwordHash");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(new RegisterRequest("USER@example.com", "password123")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void loginNormalizesEmailAndReturnsBearerToken() {
        User user = User.create("user@example.com", "encoded");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtTokenService.createToken(user)).thenReturn("signed.jwt.token");
        when(jwtTokenService.expiresInSeconds()).thenReturn(3600L);

        var response = authenticationService.login(new LoginRequest("USER@Example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsIncorrectCredentialsWithoutRevealingWhichPartFailed() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");
    }

    @Test
    void rejectsBcryptPasswordsLongerThanItsSupportedUtf8Limit() {
        String password = "é".repeat(37);

        assertThatThrownBy(() -> authenticationService.register(new RegisterRequest("user@example.com", password)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("72 UTF-8 bytes");

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user@example.com", password)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("72 UTF-8 bytes");
    }
}