package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.UserRegistrationRequest;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.service.TenantService;
import com.steve.saasapp.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantService tenantService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private Tenant tenant;
    private UserRegistrationRequest request;
    private User savedUser;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("Acme Corp").build();

        request = new UserRegistrationRequest();
        request.setName("Alice Smith");
        request.setEmail("alice@acme.com");
        request.setPassword("secret123");
        request.setTenantName("Acme Corp");
        request.setRole(User.Role.ADMIN);

        savedUser = User.builder()
                .id(1L).name("Alice Smith").email("alice@acme.com")
                .password("hashed_password").tenant(tenant)
                .role(User.Role.ADMIN).build();
    }

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("registers user successfully with all fields")
        void registerUser_success() {
            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.empty());
            when(tenantService.getTenantByName("Acme Corp")).thenReturn(tenant);
            when(passwordEncoder.encode("secret123")).thenReturn("hashed_password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.registerUser(request);

            assertThat(result.getEmail()).isEqualTo("alice@acme.com");
            assertThat(result.getName()).isEqualTo("Alice Smith");
            assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
            assertThat(result.getTenant().getName()).isEqualTo("Acme Corp");
            verify(passwordEncoder).encode("secret123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("defaults role to MEMBER when role is not provided")
        void registerUser_defaultsToMember() {
            request.setRole(null);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(tenantService.getTenantByName(anyString())).thenReturn(tenant);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getRole()).isEqualTo(User.Role.MEMBER);
                return savedUser;
            });

            userService.registerUser(request);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws RuntimeException when email is already registered")
        void registerUser_duplicateEmail() {
            when(userRepository.findByEmail("alice@acme.com"))
                    .thenReturn(Optional.of(savedUser));

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RuntimeException when tenant does not exist")
        void registerUser_tenantNotFound() {
            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.empty());
            when(tenantService.getTenantByName("Acme Corp"))
                    .thenThrow(new RuntimeException("Tenant not found: Acme Corp"));

            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Tenant not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("password is encoded before saving — never stored in plain text")
        void registerUser_passwordIsEncoded() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(tenantService.getTenantByName(anyString())).thenReturn(tenant);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getPassword()).isEqualTo("$2a$10$hashed");
                assertThat(u.getPassword()).isNotEqualTo("secret123");
                return savedUser;
            });

            userService.registerUser(request);
        }
    }
}
