package com.steve.saasapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be at least 32 bytes for HMAC-SHA
    private static final String SECRET = "test-secret-key-that-is-32-chars!!";
    private static final long EXPIRATION = 1000 * 60 * 60; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("generates a non-null, non-empty token")
        void generateToken_returnsToken() {
            String token = jwtUtil.generateToken("alice@acme.com");

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("generates different tokens for different emails")
        void generateToken_differentEmailsDifferentTokens() {
            String token1 = jwtUtil.generateToken("alice@acme.com");
            String token2 = jwtUtil.generateToken("bob@acme.com");

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractEmail")
    class ExtractEmail {

        @Test
        @DisplayName("extracts the correct email from a valid token")
        void extractEmail_returnsCorrectEmail() {
            String token = jwtUtil.generateToken("alice@acme.com");

            String email = jwtUtil.extractEmail(token);

            assertThat(email).isEqualTo("alice@acme.com");
        }

        @Test
        @DisplayName("throws exception for a tampered token")
        void extractEmail_tamperedToken_throws() {
            String token = jwtUtil.generateToken("alice@acme.com");
            String tampered = token + "tampered";

            assertThatThrownBy(() -> jwtUtil.extractEmail(tampered))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("returns true for a freshly generated token")
        void isTokenValid_validToken_returnsTrue() {
            String token = jwtUtil.generateToken("alice@acme.com");

            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for a tampered token")
        void isTokenValid_tamperedToken_returnsFalse() {
            String token = jwtUtil.generateToken("alice@acme.com");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("returns false for a completely invalid string")
        void isTokenValid_randomString_returnsFalse() {
            assertThat(jwtUtil.isTokenValid("not.a.jwt.token")).isFalse();
        }

        @Test
        @DisplayName("returns false for an expired token")
        void isTokenValid_expiredToken_returnsFalse() {
            // Create a JwtUtil with 1ms expiration
            JwtUtil shortLivedJwt = new JwtUtil(SECRET, 1L);
            String token = shortLivedJwt.generateToken("alice@acme.com");

            // Wait for expiration
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(shortLivedJwt.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("returns false for empty string")
        void isTokenValid_emptyString_returnsFalse() {
            assertThat(jwtUtil.isTokenValid("")).isFalse();
        }
    }
}
