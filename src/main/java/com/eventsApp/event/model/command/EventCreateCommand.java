package com.eventsApp.event.model.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EventCreateCommand {
    private String clientPersonalData;
    private String venue;
    private LocalDate date;
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;
    private BigDecimal price;
    private String comment;
}
