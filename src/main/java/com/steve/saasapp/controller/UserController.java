package com.steve.saasapp.controller;

import com.steve.saasapp.dto.UserRegistrationRequest;
import com.steve.saasapp.dto.UserResponseDTO;
import com.steve.saasapp.model.User;
import com.steve.saasapp.security.CustomUserDetails;
import com.steve.saasapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        User registered = userService.registerUser(request);
        UserResponseDTO response = new UserResponseDTO(
                registered.getName(),
                registered.getEmail(),
                registered.getRole().toString(),
                registered.getTenant().getName(),
                registered.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        UserResponseDTO response = new UserResponseDTO(
                user.getName(),
                user.getEmail(),
                user.getRole().toString(),
                user.getTenant().getName(),
                user.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }
}