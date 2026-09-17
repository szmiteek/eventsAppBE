package com.eventsApp.publicform.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicTenantInfoDTO {
    private String companyName;
    /** Tells the form whether to show the logo, so it never renders a broken image. */
    private boolean hasLogo;
}
