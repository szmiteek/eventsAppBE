package com.eventsApp.offer;

import com.eventsApp.employee.model.Employee;
import com.eventsApp.employee.model.command.EmployeeUpdateCommand;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;

import java.util.Optional;

public class OfferMapper {
    public static Offer fromCreateCommand(OfferCreateCommand dto) {
        return Offer.builder()
                .personalData(dto.getPersonalData())
                .venue(dto.getVenue())
                .eventDate(dto.getEventDate())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .budget(dto.getBudget())
                .guests(dto.getGuests())
                .build();
    }

    public static OfferDTO mapToDTO(Offer dto) {
        return OfferDTO.builder()
                .id(dto.getId())
                .createdDate(dto.getCreatedDate())
                .personalData(dto.getPersonalData())
                .venue(dto.getVenue())
                .eventDate(dto.getEventDate())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .budget(dto.getBudget())
                .guests(dto.getGuests())
                .price(dto.getPrice() != null ? dto.getPrice() : null)
                .comment(dto.getComment() != null ? dto.getComment() : null)
                .status(dto.getStatus())
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
    }
}
