package com.eventsApp.eventElement;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.event.EventRepository;
import com.eventsApp.event.model.Event;
import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.eventElement.model.command.EventElementCreateCommand;
import com.eventsApp.eventElement.model.command.EventElementUpdateCommand;
import com.eventsApp.eventElement.model.dto.EventElementDTO;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.model.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.eventsApp.eventElement.EventElementMapper.fromCreateCommand;
import static com.eventsApp.eventElement.EventElementMapper.mapToDTO;
import static com.eventsApp.eventElement.EventElementMapper.updateFromCommand;

@Service
@RequiredArgsConstructor
public class EventElementService {

    private final EventElementRepository eventElementRepository;
    private final OfferRepository offerRepository;
    private final EventRepository eventRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public EventElementDTO create(EventElementCreateCommand command) {
        if ((command.getOfferId() == null) == (command.getEventId() == null)) {
            throw new EventApiException("Podaj dokładnie jedno: offerId albo eventId.", HttpStatus.BAD_REQUEST);
        }

        EventElement element = fromCreateCommand(command);
        if (command.getOfferId() != null) {
            element.setOffer(getOwnedOffer(command.getOfferId()));
        } else {
            element.setEvent(getOwnedEvent(command.getEventId()));
        }

        EventElement saved = eventElementRepository.save(element);
        recalculateParentPrice(saved);
        return mapToDTO(saved);
    }

    public EventElementDTO update(int id, EventElementUpdateCommand command) {
        EventElement element = eventElementRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event element not found", HttpStatus.NOT_FOUND));
        requireOwnedParent(element);
        updateFromCommand(element, command);
        EventElement saved = eventElementRepository.save(element);
        recalculateParentPrice(saved);
        return mapToDTO(saved);
    }

    public void delete(int id) {
        EventElement element = eventElementRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event element not found", HttpStatus.NOT_FOUND));
        requireOwnedParent(element);
        Integer offerId = element.getOffer() != null ? element.getOffer().getId() : null;
        Integer eventId = element.getEvent() != null ? element.getEvent().getId() : null;

        eventElementRepository.deleteById(id);

        if (offerId != null) {
            recalculateOfferPrice(offerId);
        } else if (eventId != null) {
            recalculateEventPrice(eventId);
        }
    }

    public List<EventElementDTO> getAllByOfferId(int offerId) {
        getOwnedOffer(offerId);
        return eventElementRepository.findAllByOfferId(offerId).stream()
                .map(EventElementMapper::mapToDTO)
                .toList();
    }

    public List<EventElementDTO> getAllByEventId(int eventId) {
        getOwnedEvent(eventId);
        return eventElementRepository.findAllByEventId(eventId).stream()
                .map(EventElementMapper::mapToDTO)
                .toList();
    }

    /** An element hangs off an offer or an event — verify the current tenant owns whichever it is. */
    private void requireOwnedParent(EventElement element) {
        if (element.getOffer() != null) {
            getOwnedOffer(element.getOffer().getId());
        } else if (element.getEvent() != null) {
            getOwnedEvent(element.getEvent().getId());
        } else {
            throw new EventApiException("Element nie jest powiązany z ofertą ani eventem.", HttpStatus.CONFLICT);
        }
    }

    private void recalculateParentPrice(EventElement element) {
        if (element.getOffer() != null) {
            recalculateOfferPrice(element.getOffer().getId());
        } else if (element.getEvent() != null) {
            recalculateEventPrice(element.getEvent().getId());
        }
    }

    private Event getOwnedEvent(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventApiException("Event not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(event.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Event not found", HttpStatus.NOT_FOUND);
        }
        return event;
    }

    private void recalculateEventPrice(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventApiException("Event not found", HttpStatus.NOT_FOUND));
        BigDecimal total = eventElementRepository.findAllByEventId(eventId).stream()
                .map(element -> element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        event.setPrice(total);
        eventRepository.save(event);
    }

    private Offer getOwnedOffer(int offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        return offer;
    }

    private void recalculateOfferPrice(int offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        BigDecimal total = eventElementRepository.findAllByOfferId(offerId).stream()
                .map(element -> element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        offer.setPrice(total);
        offerRepository.save(offer);
    }
}
