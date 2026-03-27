package com.steve.saasapp.service.impl;

import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.repository.TenantRepository;
import com.steve.saasapp.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Autowired
    public TenantServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Tenant registerTenant(String name) {
        Optional<Tenant> existing = tenantRepository.findByName(name);
        if (existing.isPresent()) {
            throw new RuntimeException("Tenant with name '" + name + "' already exists.");
        }
        Tenant tenant = Tenant.builder()
                .name(name)
                .build();
        return tenantRepository.save(tenant);
    }

    @Override
    public Tenant getTenantByName(String name) {
        return tenantRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + name));
    }
}

