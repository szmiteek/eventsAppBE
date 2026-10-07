package com.eventsApp.event.model.command;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventUpdateCommand {
    private BigDecimal price;

    /**
     * The switch from the UI. Sent whenever the deposit changes, because a null amount alone means
     * "leave it alone" here — without the flag there would be no way to mark a deposit as unpaid again.
     */
    private Boolean depositPaid;
    private BigDecimal depositAmount;

    private String comment;
    private String decorationDescription;
}
