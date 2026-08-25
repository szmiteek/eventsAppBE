package com.eventsApp.offer.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OfferFilter {
    private LocalDate createdDateFrom;
    private LocalDate createdDateTo;
    private LocalDate eventDateFrom;
    private LocalDate eventDateTo;
    private String client;
    private String venue;
}
