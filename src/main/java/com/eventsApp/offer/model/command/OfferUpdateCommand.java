package com.eventsApp.offer.model.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    private List<String> eventType;
    private String mainTableType;
    private String mainTableSeats;
    private String guestsTableType;
    private Boolean appetizersOnTable;
    private List<String> decorationType;
    private String flowersType;
    private String colors;
    private String description;
}
