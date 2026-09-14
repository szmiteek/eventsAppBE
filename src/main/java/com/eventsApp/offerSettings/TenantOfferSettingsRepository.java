package com.eventsApp.offerSettings;

import com.eventsApp.offerSettings.model.TenantOfferSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantOfferSettingsRepository extends JpaRepository<TenantOfferSettings, Integer> {

    Optional<TenantOfferSettings> findByTenantId(int tenantId);
}
