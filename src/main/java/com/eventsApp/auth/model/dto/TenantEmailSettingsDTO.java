package com.eventsApp.auth.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantEmailSettingsDTO {
    private String emailMessage;
}
