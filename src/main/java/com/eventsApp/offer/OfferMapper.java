package com.eventsApp.offer;

import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;

import java.util.Optional;

public class OfferMapper {
    public static Offer fromCreateCommand(OfferCreateCommand command) {
        return Offer.builder()
                .personalData(command.getPersonalData())
                .venue(command.getVenue())
                .eventDate(command.getEventDate())
                .email(command.getEmail())
                .phone(command.getPhone())
                .budget(command.getBudget())
                .guests(command.getGuests())
                .eventType(command.getEventType())
                .mainTableType(command.getMainTableType())
                .mainTableSeats(command.getMainTableSeats())
                .guestsTableType(command.getGuestsTableType())
                .appetizersOnTable(command.isAppetizersOnTable())
                .decorationType(command.getDecorationType())
                .flowersType(command.getFlowersType())
                .colors(command.getColors())
                .description(command.getDescription())
                .build();
    }

    public static OfferDTO mapToDTO(Offer offer) {
        return OfferDTO.builder()
                .id(offer.getId())
                .createdDate(offer.getCreatedDate())
                .personalData(offer.getPersonalData())
                .venue(offer.getVenue())
                .eventDate(offer.getEventDate())
                .email(offer.getEmail())
                .phone(offer.getPhone())
                .budget(offer.getBudget())
                .guests(offer.getGuests())
                .price(offer.getPrice() != null ? offer.getPrice() : null)
                .comment(offer.getComment() != null ? offer.getComment() : null)
                .status(offer.getStatus())
                .eventType(offer.getEventType())
                .mainTableType(offer.getMainTableType())
                .mainTableSeats(offer.getMainTableSeats())
                .guestsTableType(offer.getGuestsTableType())
                .appetizersOnTable(offer.isAppetizersOnTable())
                .decorationType(offer.getDecorationType())
                .flowersType(offer.getFlowersType())
                .colors(offer.getColors())
                .description(offer.getDescription())
                .build();
    }

    public static void updateFromCommand(Offer offer, OfferUpdateCommand command) {
        Optional.ofNullable(command.getPersonalData()).ifPresent(offer::setPersonalData);
        Optional.ofNullable(command.getEventDate()).ifPresent(offer::setEventDate);
        Optional.ofNullable(command.getVenue()).ifPresent(offer::setVenue);
        Optional.ofNullable(command.getPersonalData()).ifPresent(offer::setPersonalData);
        Optional.ofNullable(command.getBudget()).ifPresent(offer::setBudget);
        Optional.ofNullable(command.getGuests()).ifPresent(offer::setGuests);
        Optional.ofNullable(command.getComment()).ifPresent(offer::setComment);
        Optional.ofNullable(command.getPrice()).ifPresent(offer::setPrice);
        Optional.ofNullable(command.getEventType()).ifPresent(offer::setEventType);
        Optional.ofNullable(command.getMainTableType()).ifPresent(offer::setMainTableType);
        Optional.ofNullable(command.getMainTableSeats()).ifPresent(offer::setMainTableSeats);
        Optional.ofNullable(command.getGuestsTableType()).ifPresent(offer::setGuestsTableType);
        Optional.ofNullable(command.getAppetizersOnTable()).ifPresent(offer::setAppetizersOnTable);
        Optional.ofNullable(command.getDecorationType()).ifPresent(offer::setDecorationType);
        Optional.ofNullable(command.getFlowersType()).ifPresent(offer::setFlowersType);
        Optional.ofNullable(command.getColors()).ifPresent(offer::setColors);
        Optional.ofNullable(command.getDescription()).ifPresent(offer::setDescription);
    }
}
