package com.eventsApp.offer.model.dto;

import com.eventsApp.offer.OfferStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OfferDTO {
    private int id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdDate;

    private String personalData;
    private String venue;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate eventDate;

    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;
    private BigDecimal price;
    private String comment;
    private OfferStatus status;
}
