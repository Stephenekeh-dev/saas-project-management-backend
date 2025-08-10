package com.steve.saasapp.dto;



import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class UserResponseDTO {
    private String name;
    private String email;
    private String role;
    private String tenantName;
    private LocalDateTime createdAt;

    public UserResponseDTO(String name, String email, String role, String tenantName, LocalDateTime createdAt) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.tenantName = tenantName;
        this.createdAt = createdAt;
    }

    // Getters and setters (or use Lombok @Data)
}
