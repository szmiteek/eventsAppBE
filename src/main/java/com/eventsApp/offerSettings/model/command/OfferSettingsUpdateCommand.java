package com.eventsApp.offerSettings.model.command;

import com.eventsApp.offerSettings.OfferSettingsService;
import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.PdfOrientation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OfferSettingsUpdateCommand {

    @NotNull
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String backgroundColor;

    @NotNull
    private PdfOrientation orientation;

    @NotNull
    @Size(min = 1, max = OfferSettingsService.MAX_INFO_FIELDS)
    private List<@NotNull OfferInfoField> infoFields;
}
