package com.eventsApp.event.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EventDTO {
    private int id;
    private String clientPersonalData;
    private String venue;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;
    private BigDecimal price;
    /** Null means unpaid — the client derives the switch from this. */
    private BigDecimal depositAmount;
    private String comment;
    private String decorationDescription;
    private boolean afterWeddingParty;
    /** Null for an event added by hand — only an event created from a signed offer points at one. */
    private Integer offerId;
}
