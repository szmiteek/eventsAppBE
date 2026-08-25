package com.eventsApp.auth;

import com.eventsApp.auth.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    Optional<Tenant> findByEmail(String email);

    Optional<Tenant> findByPublicFormToken(String publicFormToken);
}
