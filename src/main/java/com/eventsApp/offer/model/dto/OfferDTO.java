package com.eventsApp.offer.model.dto;

import com.eventsApp.offer.OfferStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    private List<String> eventType;
    private String mainTableType;
    private String mainTableSeats;
    private String guestsTableType;
    private boolean appetizersOnTable;
    private List<String> decorationType;
    private String flowersType;
    private String colors;
    private String description;
}
