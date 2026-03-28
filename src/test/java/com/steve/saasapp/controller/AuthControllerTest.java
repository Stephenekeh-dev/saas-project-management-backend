package com.steve.saasapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.AuthResponse;
import com.steve.saasapp.dto.LoginRequest;
import com.steve.saasapp.dto.RefreshTokenRequest;
import com.steve.saasapp.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;

    // ─── Login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login - returns 200 with access and refresh tokens")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@acme.com");
        request.setPassword("secret123");

        when(authService.login(any()))
                .thenReturn(new AuthResponse("eyJhbGci.access.token", "uuid-refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("eyJhbGci.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("uuid-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 500 on invalid credentials")
    void login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@acme.com");
        request.setPassword("wrongpassword");

        when(authService.login(any()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 500 when user not found")
    void login_userNotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@acme.com");
        request.setPassword("secret123");

        when(authService.login(any()))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/refresh - returns 200 with new access token")
    void refresh_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refresh(any()))
                .thenReturn(new AuthResponse("eyJhbGci.new.access.token", "valid-refresh-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("eyJhbGci.new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - returns 500 when refresh token is expired")
    void refresh_expiredToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(authService.refresh(any()))
                .thenThrow(new RuntimeException("Refresh token has expired. Please log in again."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - returns 500 when refresh token not found")
    void refresh_tokenNotFound() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("nonexistent-token");

        when(authService.refresh(any()))
                .thenThrow(new RuntimeException("Refresh token not found"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/logout - returns 204 on successful logout")
    void logout_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/auth/logout - returns 500 when refresh token is invalid")
    void logout_invalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        doThrow(new RuntimeException("Refresh token not found"))
                .when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}