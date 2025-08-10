package com.steve.saasapp.service;
import com.steve.saasapp.model.Tenant;

public interface TenantService {
    Tenant registerTenant(String name);
    Tenant getTenantByName(String name);
}
