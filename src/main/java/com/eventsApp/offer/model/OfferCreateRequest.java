package com.eventsApp.offer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class OfferCreateRequest {
    private String personalData;
    private String venue;
    private LocalDate date;
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;

}
