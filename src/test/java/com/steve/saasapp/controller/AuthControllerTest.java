package com.steve.saasapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.LoginRequest;
import com.steve.saasapp.dto.LoginResponse;
import com.steve.saasapp.security.JwtAuthenticationFilter;
import com.steve.saasapp.security.JwtUtil;
import com.steve.saasapp.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtUtil jwtUtil;                          // ← add this
    @MockitoBean private JwtAuthenticationFilter jwtAuthFilter;   // ← add this

    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 and JWT token on valid credentials")
        void login_success() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("alice@acme.com");
            request.setPassword("secret123");

            when(authService.login(any())).thenReturn(new LoginResponse("eyJhbGci.test.token"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGci.test.token"));
        }

        @Test
        @DisplayName("returns 500 when credentials are invalid")
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
    }
}