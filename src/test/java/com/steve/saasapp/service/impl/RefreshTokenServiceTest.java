package com.steve.saasapp.service.impl;



import com.steve.saasapp.model.RefreshToken;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.RefreshTokenRepository;
import com.steve.saasapp.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken validToken;
    private RefreshToken expiredToken;

    @BeforeEach
    void setUp() {
        Tenant tenant = Tenant.builder().id(1L).name("Acme Corp").build();
        user = User.builder()
                .id(1L).name("Alice").email("alice@acme.com")
                .tenant(tenant).role(User.Role.ADMIN).build();

        validToken = RefreshToken.builder()
                .id(1L).user(user)
                .token("valid-uuid-token")
                .expiry(LocalDateTime.now().plusDays(7))
                .build();

        expiredToken = RefreshToken.builder()
                .id(2L).user(user)
                .token("expired-uuid-token")
                .expiry(LocalDateTime.now().minusDays(1)) // already expired
                .build();
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("deletes existing token and creates a new one for the user")
        void createRefreshToken_success() {
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

            RefreshToken result = refreshTokenService.createRefreshToken(user);

            verify(refreshTokenRepository).deleteByUser(user);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            assertThat(result.getToken()).isEqualTo("valid-uuid-token");
            assertThat(result.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("saved token expiry is 7 days in the future")
        void createRefreshToken_expiryIsSevenDays() {
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken t = inv.getArgument(0);
                assertThat(t.getExpiry()).isAfter(LocalDateTime.now().plusDays(6));
                assertThat(t.getExpiry()).isBefore(LocalDateTime.now().plusDays(8));
                return t;
            });

            refreshTokenService.createRefreshToken(user);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken")
    class ValidateRefreshToken {

        @Test
        @DisplayName("returns token when it exists and is not expired")
        void validateRefreshToken_valid() {
            when(refreshTokenRepository.findByToken("valid-uuid-token"))
                    .thenReturn(Optional.of(validToken));

            RefreshToken result = refreshTokenService.validateRefreshToken("valid-uuid-token");

            assertThat(result.getToken()).isEqualTo("valid-uuid-token");
        }

        @Test
        @DisplayName("throws RuntimeException when token does not exist")
        void validateRefreshToken_notFound() {
            when(refreshTokenRepository.findByToken("unknown-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("unknown-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Refresh token not found");
        }

        @Test
        @DisplayName("throws RuntimeException and deletes token when it is expired")
        void validateRefreshToken_expired() {
            when(refreshTokenRepository.findByToken("expired-uuid-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() ->
                    refreshTokenService.validateRefreshToken("expired-uuid-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("deleteByUser")
    class DeleteByUser {

        @Test
        @DisplayName("calls repository deleteByUser with the correct user")
        void deleteByUser_success() {
            doNothing().when(refreshTokenRepository).deleteByUser(user);

            refreshTokenService.deleteByUser(user);

            verify(refreshTokenRepository).deleteByUser(user);
        }
    }
}
