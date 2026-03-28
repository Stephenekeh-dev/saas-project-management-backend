package com.steve.saasapp.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.steve.saasapp.dto.TenantRegistrationRequest;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.service.TenantService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TenantController")
class TenantControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private TenantService tenantService;

    @Nested
    @DisplayName("POST /api/tenants/register")
    class RegisterTenant {

        @Test
        @DisplayName("returns 200 and registered tenant on success")
        void registerTenant_success() throws Exception {
            TenantRegistrationRequest request = new TenantRegistrationRequest();
            request.setName("Acme Corp");

            Tenant tenant = Tenant.builder()
                    .id(1L)
                    .name("Acme Corp")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(tenantService.registerTenant("Acme Corp")).thenReturn(tenant);

            mockMvc.perform(post("/api/tenants/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Acme Corp"))
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("returns 500 when tenant name already exists")
        void registerTenant_duplicate() throws Exception {
            TenantRegistrationRequest request = new TenantRegistrationRequest();
            request.setName("Acme Corp");

            when(tenantService.registerTenant("Acme Corp"))
                    .thenThrow(new RuntimeException("Tenant with name 'Acme Corp' already exists."));

            mockMvc.perform(post("/api/tenants/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("returns 400 when tenant name is blank")
        void registerTenant_blankName() throws Exception {
            TenantRegistrationRequest request = new TenantRegistrationRequest();
            request.setName("");

            mockMvc.perform(post("/api/tenants/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is missing name field")
        void registerTenant_missingName() throws Exception {
            mockMvc.perform(post("/api/tenants/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}