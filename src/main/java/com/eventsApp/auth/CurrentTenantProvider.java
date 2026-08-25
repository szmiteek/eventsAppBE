package com.eventsApp.auth;

import com.eventsApp.auth.model.TenantRole;
import com.eventsApp.exceptions.EventApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantProvider {

    public AuthenticatedTenant getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedTenant principal)) {
            throw new EventApiException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public int requireTenantId() {
        AuthenticatedTenant current = getCurrent();
        if (current.role() != TenantRole.TENANT) {
            throw new EventApiException("Tenant access required", HttpStatus.FORBIDDEN);
        }
        return current.tenantId();
    }
}
