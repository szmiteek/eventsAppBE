package com.eventsApp.event.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EventFilter {
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String client;
    private String venue;
}
