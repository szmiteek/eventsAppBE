package com.eventsApp.offer.model.command;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class OfferSendCommand {

    /** Optional override — lets the user fix a typo in the client's address before sending. */
    @Email(message = "NOT_EMAIL")
    private String email;
}
