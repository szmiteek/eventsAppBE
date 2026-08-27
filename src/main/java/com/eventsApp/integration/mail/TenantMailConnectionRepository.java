package com.eventsApp.integration.mail;

import com.eventsApp.integration.mail.model.TenantMailConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantMailConnectionRepository extends JpaRepository<TenantMailConnection, Integer> {

    Optional<TenantMailConnection> findByTenantId(Integer tenantId);
}
