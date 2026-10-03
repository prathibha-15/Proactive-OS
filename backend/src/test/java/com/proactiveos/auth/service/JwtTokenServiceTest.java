package com.proactiveos.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.proactiveos.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

    @Test
    void createsVerifiableTokenWithUserIdentityAndExpiry() {
        byte[] secret = "phase-7-test-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8);
        var key = new SecretKeySpec(secret, "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        ((NimbusJwtDecoder) decoder).setJwtValidator(JwtValidators.createDefaultWithIssuer("proactive-os"));
        JwtTokenService tokenService = new JwtTokenService(encoder, Duration.ofHours(1), "proactive-os");
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("user@example.com");

        var token = decoder.decode(tokenService.createToken(user));

        assertThat(token.getSubject()).isEqualTo("42");
        assertThat(token.getClaimAsString("email")).isEqualTo("user@example.com");
        assertThat(token.getClaimAsString("iss")).isEqualTo("proactive-os");
        assertThat(token.getExpiresAt()).isAfter(token.getIssuedAt());
        assertThat(tokenService.expiresInSeconds()).isEqualTo(3600L);
    }
}