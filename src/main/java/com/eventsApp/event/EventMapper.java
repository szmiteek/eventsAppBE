package com.eventsApp.event;

import com.eventsApp.event.model.Event;
import com.eventsApp.event.model.command.EventCreateCommand;
import com.eventsApp.event.model.dto.EventDTO;
import com.eventsApp.offer.model.Offer;

public class EventMapper {
    public static Event fromEventCreateCommand(EventCreateCommand command) {
        return Event.builder()
                .clientPersonalData(command.getClientPersonalData())
                .venue(command.getVenue())
                .date(command.getDate())
                .email(command.getEmail())
                .phone(command.getPhone())
                .budget(command.getBudget())
                .guests(command.getGuests())
                .price(command.getPrice())
                .comment(command.getComment())
                .build();
    }

    public static Event fromOffer(Offer offer) {
        return Event.builder()
                .clientPersonalData(offer.getPersonalData())
                .venue(offer.getVenue())
                .date(offer.getEventDate())
                .email(offer.getEmail())
                .phone(offer.getPhone())
                .budget(offer.getBudget())
                .guests(offer.getGuests())
                .price(offer.getPrice())
                .comment(offer.getComment())
                .decorationDescription(offer.getDecorationDescription())
                .offerId(offer.getId())
                .tenantId(offer.getTenantId())
                .build();
    }

    public static EventDTO mapToDTO(Event event) {
        return EventDTO.builder()
                .id(event.getId())
                .clientPersonalData(event.getClientPersonalData())
                .venue(event.getVenue())
                .date(event.getDate())
                .email(event.getEmail())
                .phone(event.getPhone())
                .budget(event.getBudget())
                .guests(event.getGuests())
                .price(event.getPrice())
                .comment(event.getComment())
                .decorationDescription(event.getDecorationDescription())
                .offerId(event.getOfferId() != null ? event.getOfferId() : null)
                .build();
    }
}
