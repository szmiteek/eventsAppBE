package com.eventsApp.publicform.model.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PublicOfferCommand {

    @NotBlank(message = "EMPTY_VALUE")
    private String personalData;

    @NotBlank(message = "EMPTY_VALUE")
    private String venue;

    @NotNull(message = "EMPTY_VALUE")
    @Future(message = "DATE_IN_PAST")
    private LocalDate eventDate;

    @NotBlank(message = "EMPTY_VALUE")
    @Email(message = "NOT_EMAIL")
    private String email;

    @NotBlank(message = "EMPTY_VALUE")
    private String phone;

    @NotNull(message = "EMPTY_VALUE")
    private Integer budget;

    @NotNull(message = "EMPTY_VALUE")
    private Integer guests;

    @NotEmpty(message = "EMPTY_VALUE")
    private List<String> eventType;

    @NotEmpty(message = "EMPTY_VALUE")
    private List<String> decorationType;

    @NotBlank(message = "EMPTY_VALUE")
    private String colors;

    @NotBlank(message = "EMPTY_VALUE")
    private String description;

    @NotBlank(message = "EMPTY_VALUE")
    private String mainTableType;

    @NotBlank(message = "EMPTY_VALUE")
    private String mainTableSeats;

    @NotBlank(message = "EMPTY_VALUE")
    private String guestsTableType;

    @NotBlank(message = "EMPTY_VALUE")
    private String flowersType;

    /** Honeypot — musi pozostać puste. Wypełnione przez bota = cichy sukces bez zapisu. */
    private String honeypot;
}
