package com.steve.saasapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.UserRegistrationRequest;
import com.steve.saasapp.dto.UserResponseDTO;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;

    private CustomUserDetails userDetails;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder()
                .id(1L).name("Acme Corp").build();

        registeredUser = User.builder()
                .id(1L).name("Alice Smith")
                .email("alice@acme.com")
                .password("hashed_password")
                .role(User.Role.ADMIN)
                .tenant(tenant)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        userDetails = new CustomUserDetails(registeredUser);
    }

    @Nested
    @DisplayName("POST /api/users/register")
    class RegisterUser {

        @Test
        @DisplayName("returns 200 and user response on success")
        void registerUser_success() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setName("Alice Smith");
            request.setEmail("alice@acme.com");
            request.setPassword("secret123");
            request.setTenantName("Acme Corp");
            request.setRole(User.Role.ADMIN);

            when(userService.registerUser(any())).thenReturn(registeredUser);

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice Smith"))
                    .andExpect(jsonPath("$.email").value("alice@acme.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.tenantName").value("Acme Corp"));
        }

        @Test
        @DisplayName("returns 500 when email is already registered")
        void registerUser_duplicateEmail() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setName("Alice Smith");
            request.setEmail("alice@acme.com");
            request.setPassword("secret123");
            request.setTenantName("Acme Corp");

            when(userService.registerUser(any()))
                    .thenThrow(new RuntimeException("Email is already registered."));

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns 400 when required fields are missing")
        void registerUser_missingFields() throws Exception {
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email format is invalid")
        void registerUser_invalidEmail() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setName("Alice Smith");
            request.setEmail("not-a-valid-email");
            request.setPassword("secret123");
            request.setTenantName("Acme Corp");

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 500 when tenant does not exist")
        void registerUser_tenantNotFound() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setName("Alice Smith");
            request.setEmail("alice@acme.com");
            request.setPassword("secret123");
            request.setTenantName("Unknown Tenant");

            when(userService.registerUser(any()))
                    .thenThrow(new RuntimeException("Tenant not found: Unknown Tenant"));

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("returns 200 and current user profile when authenticated")
        void getCurrentUser_success() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Alice Smith"))
                    .andExpect(jsonPath("$.email").value("alice@acme.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.tenantName").value("Acme Corp"));
        }

        @Test
        @DisplayName("returns 403 when not authenticated")
        void getCurrentUser_unauthenticated() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());
        }
    }
}