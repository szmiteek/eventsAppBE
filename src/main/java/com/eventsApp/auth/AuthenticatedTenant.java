package com.eventsApp.auth;

import com.eventsApp.auth.model.TenantRole;

public record AuthenticatedTenant(Integer tenantId, TenantRole role) {
}
