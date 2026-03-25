package com.steve.saasapp.security;


import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
        user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .password("hashed").tenant(tenant).role(User.Role.ADMIN).build();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("sets authentication when token is valid")
        void validToken_setsAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
            when(jwtUtil.extractEmail("valid.token.here")).thenReturn("alice@acme.com");
            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
            when(jwtUtil.isTokenValid("valid.token.here")).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isInstanceOf(CustomUserDetails.class);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("does not set authentication when Authorization header is missing")
        void missingHeader_noAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil, userRepository);
        }

        @Test
        @DisplayName("does not set authentication when Authorization header does not start with Bearer")
        void nonBearerHeader_noAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil, userRepository);
        }

        @Test
        @DisplayName("does not set authentication when token is invalid")
        void invalidToken_noAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
            when(jwtUtil.extractEmail("bad.token")).thenReturn("alice@acme.com");
            when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
            when(jwtUtil.isTokenValid("bad.token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("does not set authentication when user is not found in database")
        void userNotFound_noAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
            when(jwtUtil.extractEmail("valid.token")).thenReturn("ghost@acme.com");
            when(userRepository.findByEmail("ghost@acme.com")).thenReturn(Optional.empty());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("always calls filterChain.doFilter regardless of auth outcome")
        void alwaysCallsFilterChain() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
