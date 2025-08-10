package com.steve.saasapp.repository;

import com.steve.saasapp.model.Project;
import com.steve.saasapp.model.Tenant;
import com.steve.saasapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Exclude soft-deleted projects
    List<Project> findByTenantAndIsDeletedFalse(Tenant tenant);

    boolean existsByNameAndTenant(String name, Tenant tenant);

    Optional<Project> findByIdAndTenant(Long id, Tenant tenant); // You may deprecate this if not used

    // Only return non-deleted projects created by user
    List<Project> findByCreatedByAndIsDeletedFalse(User user);

    // Used in pagination and filtering
    Page<Project> findByTenantIdAndNameContainingIgnoreCaseAndStatusContainingIgnoreCaseAndIsDeletedFalse(
            Long tenantId,
            String name,
            String status,
            Pageable pageable
    );

    // For get-by-ID that excludes deleted
    Optional<Project> findByIdAndIsDeletedFalse(Long id);

    Optional<Project> findByIdAndTenantIdAndIsDeletedFalse(Long id, Long tenantId);
}