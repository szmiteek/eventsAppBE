package com.eventsApp.offer.model.command;

import com.eventsApp.offer.OfferStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfferUpdateStatusCommand {
    @NotNull(message = "NULL_VALUE")
    private OfferStatus status;
}
