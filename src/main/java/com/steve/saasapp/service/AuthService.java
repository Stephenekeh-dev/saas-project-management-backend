package com.steve.saasapp.service;

import com.steve.saasapp.dto.AuthResponse;
import com.steve.saasapp.dto.LoginRequest;
import com.steve.saasapp.dto.RefreshTokenRequest;
import com.steve.saasapp.model.RefreshToken;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(
                request.getRefreshToken());

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getEmail());

        return new AuthResponse(newAccessToken, refreshToken.getToken());
    }

    public void logout(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(
                request.getRefreshToken());
        refreshTokenService.deleteByUser(refreshToken.getUser());
    }
}