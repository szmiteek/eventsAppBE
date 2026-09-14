package com.eventsApp.offerSettings.model.dto;

import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.PdfOrientation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OfferSettingsDTO {
    private String backgroundColor;
    private PdfOrientation orientation;
    private List<OfferInfoField> infoFields;
    private boolean hasLogo;
    /** Every field the tenant can choose from, with its label — the settings page renders straight from this. */
    private List<OfferInfoFieldDTO> availableFields;
    private int maxInfoFields;
}
