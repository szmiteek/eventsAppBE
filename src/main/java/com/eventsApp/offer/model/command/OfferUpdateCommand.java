package com.eventsApp.offer.model.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OfferUpdateCommand {
    private String personalData;
    private String venue;

    @Future(message = "DATE_IN_PAST")
    private LocalDate eventDate;

    @Email(message = "NOT_EMAIL")
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;
    private String comment;
    private BigDecimal price;
}
