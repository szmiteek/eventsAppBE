package com.eventsApp.eventElement.model.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventElementCreateCommand {

    /** Exactly one of offerId / eventId must be set — the element belongs to an offer or to an event. */
    private Integer offerId;

    private Integer eventId;

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal unitPrice;
}
