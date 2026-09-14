package com.eventsApp.eventElement;

import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.eventElement.model.command.EventElementCreateCommand;
import com.eventsApp.eventElement.model.command.EventElementUpdateCommand;
import com.eventsApp.eventElement.model.dto.EventElementDTO;

import java.math.BigDecimal;

public class EventElementMapper {

    public static EventElement fromCreateCommand(EventElementCreateCommand command) {
        return EventElement.builder()
                .name(command.getName())
                .quantity(command.getQuantity())
                .unitPrice(command.getUnitPrice())
                .position(command.getPosition())
                .build();
    }

    public static void updateFromCommand(EventElement element, EventElementUpdateCommand command) {
        element.setName(command.getName());
        element.setQuantity(command.getQuantity());
        element.setUnitPrice(command.getUnitPrice());
        if (command.getPosition() != null) {
            element.setPosition(command.getPosition());
        }
    }

    public static EventElementDTO mapToDTO(EventElement element) {
        return EventElementDTO.builder()
                .id(element.getId())
                .offerId(element.getOffer() != null ? element.getOffer().getId() : null)
                .eventId(element.getEvent() != null ? element.getEvent().getId() : null)
                .name(element.getName())
                .quantity(element.getQuantity())
                .unitPrice(element.getUnitPrice())
                .sum(element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity())))
                .position(element.getPosition())
                .build();
    }
}
