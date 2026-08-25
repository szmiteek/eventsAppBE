package com.eventsApp.auth.model.dto;

import com.eventsApp.auth.model.TenantRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private Integer tenantId;
    private String companyName;
    private TenantRole role;
    private String publicFormToken;
}
