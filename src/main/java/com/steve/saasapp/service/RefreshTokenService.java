package com.steve.saasapp.service;

import com.steve.saasapp.model.RefreshToken;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    // 7 days
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete any existing refresh token for this user — one at a time
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiry(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .build();

        return refreshTokenRepository.save(token);
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired. Please log in again.");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}