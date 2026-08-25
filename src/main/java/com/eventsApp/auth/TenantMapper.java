package com.eventsApp.auth;

import com.eventsApp.auth.model.Tenant;
import com.eventsApp.auth.model.dto.TenantDTO;

public class TenantMapper {

    public static TenantDTO mapToDTO(Tenant tenant) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .companyName(tenant.getCompanyName())
                .email(tenant.getEmail())
                .role(tenant.getRole())
                .active(tenant.isActive())
                .createdDate(tenant.getCreatedDate())
                .publicFormToken(tenant.getPublicFormToken())
                .build();
    }
}
