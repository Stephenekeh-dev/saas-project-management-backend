package com.steve.saasapp.service.impl;

import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.repository.TenantRepository;
import com.steve.saasapp.service.TenantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceImpl")
class TenantServiceImplTest {

    @Mock private TenantRepository tenantRepository;
    @InjectMocks private TenantServiceImpl tenantService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("Acme Corp").build();
    }

    @Nested
    @DisplayName("registerTenant")
    class RegisterTenant {

        @Test
        @DisplayName("registers and returns new tenant successfully")
        void registerTenant_success() {
            when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.empty());
            when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

            Tenant result = tenantService.registerTenant("Acme Corp");

            assertThat(result.getName()).isEqualTo("Acme Corp");
            verify(tenantRepository).save(any(Tenant.class));
        }

        @Test
        @DisplayName("throws RuntimeException when tenant name already exists")
        void registerTenant_duplicate_throwsException() {
            when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.of(tenant));

            assertThatThrownBy(() -> tenantService.registerTenant("Acme Corp"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already exists");

            verify(tenantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTenantByName")
    class GetTenantByName {

        @Test
        @DisplayName("returns tenant when it exists")
        void getTenantByName_success() {
            when(tenantRepository.findByName("Acme Corp")).thenReturn(Optional.of(tenant));

            Tenant result = tenantService.getTenantByName("Acme Corp");

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("throws RuntimeException when tenant does not exist")
        void getTenantByName_notFound() {
            when(tenantRepository.findByName("Unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.getTenantByName("Unknown"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tenant not found");
        }
    }
}
