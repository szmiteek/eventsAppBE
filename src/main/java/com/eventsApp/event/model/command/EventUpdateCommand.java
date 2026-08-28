package com.eventsApp.event.model.command;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventUpdateCommand {
    private BigDecimal price;
    private String comment;
    private String decorationDescription;
}
