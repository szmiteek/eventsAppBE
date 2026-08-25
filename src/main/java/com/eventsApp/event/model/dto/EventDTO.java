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
    private String comment;
    private int offerId;
}
