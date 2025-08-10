package com.steve.saasapp.repository;

import com.steve.saasapp.model.User;
import com.steve.saasapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByTenant(Tenant tenant);
}