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
                .build();
    }

    public static void updateFromCommand(EventElement element, EventElementUpdateCommand command) {
        element.setName(command.getName());
        element.setQuantity(command.getQuantity());
        element.setUnitPrice(command.getUnitPrice());
    }

    public static EventElementDTO mapToDTO(EventElement element) {
        return EventElementDTO.builder()
                .id(element.getId())
                .offerId(element.getOffer().getId())
                .name(element.getName())
                .quantity(element.getQuantity())
                .unitPrice(element.getUnitPrice())
                .sum(element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity())))
                .build();
    }
}
