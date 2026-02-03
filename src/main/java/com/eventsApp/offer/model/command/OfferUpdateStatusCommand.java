package com.eventsApp.offer.model.command;

import com.eventsApp.offer.OfferStatus;
import lombok.Data;

@Data
public class OfferUpdateStatusCommand {
    private OfferStatus status;
}
