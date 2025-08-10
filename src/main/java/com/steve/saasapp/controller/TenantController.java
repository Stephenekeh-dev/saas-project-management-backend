package com.steve.saasapp.controller;
import com.steve.saasapp.dto.TenantRegistrationRequest;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;


    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/register")
    public ResponseEntity<Tenant> registerTenant(
            @Valid @RequestBody TenantRegistrationRequest request) {
        Tenant tenant = tenantService.registerTenant(request.getName());
        return ResponseEntity.ok(tenant);
    }
}