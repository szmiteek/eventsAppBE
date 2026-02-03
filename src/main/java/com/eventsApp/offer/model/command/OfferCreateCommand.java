package com.eventsApp.offer.model.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OfferCreateCommand {
    @NotBlank(message = "EMPTY_VALUE")
    private String personalData;

    @NotBlank(message = "EMPTY_VALUE")
    private String venue;

    @Future(message = "DATE_IN_PAST")
    private LocalDate eventDate;

    @NotBlank(message = "EMPTY_VALUE")
    @Email(message = "NOT_EMAIL")
    private String email;

    @NotBlank(message = "EMPTY_VALUE")
    private String phone;

    @NotBlank(message = "EMPTY_VALUE")
    private Integer budget;

    @NotBlank(message = "EMPTY_VALUE")
    private Integer guests;

}
