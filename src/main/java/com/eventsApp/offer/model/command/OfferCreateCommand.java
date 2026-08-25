package com.eventsApp.offer.model.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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

    @NotNull
    private Integer budget;

    @NotNull
    private Integer guests;

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
