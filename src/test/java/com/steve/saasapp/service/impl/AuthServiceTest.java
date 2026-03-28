package com.steve.saasapp.service.impl;


import com.steve.saasapp.dto.AuthResponse;
import com.steve.saasapp.dto.LoginRequest;
import com.steve.saasapp.dto.RefreshTokenRequest;
import com.steve.saasapp.model.RefreshToken;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.security.JwtUtil;
import com.steve.saasapp.service.AuthService;
import com.steve.saasapp.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();

        user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .password("hashed_password").tenant(tenant)
                .role(User.Role.ADMIN).build();

        refreshToken = RefreshToken.builder()
                .id(1L).user(user)
                .token("uuid-refresh-token")
                .expiry(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns AuthResponse with access and refresh tokens on valid credentials")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@acme.com");
            request.setPassword("secret123");

            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);
            when(jwtUtil.generateToken("alice@acme.com")).thenReturn("eyJhbGci.access.token");
            when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("eyJhbGci.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("uuid-refresh-token");
        }

        @Test
        @DisplayName("throws RuntimeException when user is not found")
        void login_userNotFound() {
            LoginRequest request = new LoginRequest();
            request.setEmail("unknown@acme.com");
            request.setPassword("secret123");

            when(userRepository.findByEmail("unknown@acme.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("throws RuntimeException when password does not match")
        void login_wrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@acme.com");
            request.setPassword("wrongpassword");

            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");

            verify(jwtUtil, never()).generateToken(anyString());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns new AuthResponse with fresh access token on valid refresh token")
        void refresh_success() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("uuid-refresh-token");

            when(refreshTokenService.validateRefreshToken("uuid-refresh-token"))
                    .thenReturn(refreshToken);
            when(jwtUtil.generateToken("alice@acme.com"))
                    .thenReturn("eyJhbGci.new.access.token");

            AuthResponse response = authService.refresh(request);

            assertThat(response.getAccessToken()).isEqualTo("eyJhbGci.new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("uuid-refresh-token");
        }

        @Test
        @DisplayName("throws RuntimeException when refresh token is invalid")
        void refresh_invalidToken() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("bad-token");

            when(refreshTokenService.validateRefreshToken("bad-token"))
                    .thenThrow(new RuntimeException("Refresh token not found"));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Refresh token not found");

            verify(jwtUtil, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("throws RuntimeException when refresh token is expired")
        void refresh_expiredToken() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired-token");

            when(refreshTokenService.validateRefreshToken("expired-token"))
                    .thenThrow(new RuntimeException(
                            "Refresh token has expired. Please log in again."));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("deletes refresh token on valid logout request")
        void logout_success() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("uuid-refresh-token");

            when(refreshTokenService.validateRefreshToken("uuid-refresh-token"))
                    .thenReturn(refreshToken);
            doNothing().when(refreshTokenService).deleteByUser(user);

            authService.logout(request);

            verify(refreshTokenService).deleteByUser(user);
        }

        @Test
        @DisplayName("throws RuntimeException when refresh token not found on logout")
        void logout_tokenNotFound() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("nonexistent-token");

            when(refreshTokenService.validateRefreshToken("nonexistent-token"))
                    .thenThrow(new RuntimeException("Refresh token not found"));

            assertThatThrownBy(() -> authService.logout(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Refresh token not found");

            verify(refreshTokenService, never()).deleteByUser(any());
        }
    }
}