package com.steve.saasapp.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantRegistrationRequest {

    @NotBlank(message = "Tenant name is required")
    private String name;
}