package com.steve.saasapp.service.impl;

import com.steve.saasapp.dto.UserRegistrationRequest;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import com.steve.saasapp.repository.UserRepository;
import com.steve.saasapp.service.TenantService;
import com.steve.saasapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered.");
        }

        Tenant tenant = tenantService.getTenantByName(request.getTenantName());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .tenant(tenant)
                .role(request.getRole() != null ? request.getRole() : User.Role.MEMBER)
                .build();

        return userRepository.save(user);
    }
}
