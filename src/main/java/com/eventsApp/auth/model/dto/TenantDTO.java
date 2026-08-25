package com.eventsApp.auth.model.dto;

import com.eventsApp.auth.model.TenantRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TenantDTO {
    private Integer id;
    private String companyName;
    private String email;
    private TenantRole role;
    private boolean active;
    private LocalDate createdDate;
    private String publicFormToken;
}
