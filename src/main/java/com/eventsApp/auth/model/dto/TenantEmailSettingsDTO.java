package com.eventsApp.auth.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantEmailSettingsDTO {
    private String emailMessage;
    /** Adres, na który faktycznie pójdą alerty — adres konta, dopóki tenant nie wpisze własnego. */
    private String alertEmail;
    private boolean alertEmailEnabled;
}
