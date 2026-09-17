package com.eventsApp.offerSettings;

import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.TenantOfferSettings;
import com.eventsApp.offerSettings.model.dto.OfferInfoFieldDTO;
import com.eventsApp.offerSettings.model.dto.OfferSettingsDTO;

import java.util.Arrays;

public class OfferSettingsMapper {

    public static OfferSettingsDTO mapToDTO(TenantOfferSettings settings) {
        return OfferSettingsDTO.builder()
                .backgroundColor(settings.getBackgroundColor())
                .orientation(settings.getOrientation())
                .infoFields(settings.getInfoFields())
                .hasLogo(settings.getLogoData() != null && settings.getLogoData().length > 0)
                .hasCoverPdf(settings.getCoverPdfFilename() != null)
                .coverPdfFilename(settings.getCoverPdfFilename())
                .coverPdfPages(settings.getCoverPdfPages())
                .availableFields(Arrays.stream(OfferInfoField.values())
                        .map(field -> new OfferInfoFieldDTO(field, field.getLabel()))
                        .toList())
                .maxInfoFields(OfferSettingsService.MAX_INFO_FIELDS)
                .build();
    }
}
