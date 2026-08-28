package com.eventsApp.event;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.event.model.Event;
import com.eventsApp.event.model.command.EventCreateCommand;
import com.eventsApp.event.model.command.EventUpdateCommand;
import com.eventsApp.event.model.dto.EventDTO;
import com.eventsApp.event.model.dto.EventFilter;
import com.eventsApp.eventElement.EventElementRepository;
import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.model.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.eventsApp.event.EventMapper.fromEventCreateCommand;
import static com.eventsApp.event.EventMapper.fromOffer;
import static com.eventsApp.event.EventMapper.mapToDTO;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventElementRepository eventElementRepository;
    private final OfferRepository offerRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public Page<EventDTO> getAll(Pageable pageable, EventFilter filters) {
        Specification<Event> spec = EventSpecification.build(filters, currentTenantProvider.requireTenantId());
        return eventRepository.findAll(spec, pageable).map(EventMapper::mapToDTO);
    }

    public EventDTO getById(int id) {
        return mapToDTO(getOwnedEvent(id));
    }

    private Event getOwnedEvent(int id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(event.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Event not found", HttpStatus.NOT_FOUND);
        }
        return event;
    }

    public EventDTO create(EventCreateCommand command) {
        Event event = fromEventCreateCommand(command);
        event.setTenantId(currentTenantProvider.requireTenantId());
        return mapToDTO(eventRepository.save(event));
    }

    @Transactional
    public EventDTO update(int id, EventUpdateCommand command) {
        Event event = getOwnedEvent(id);
        Optional.ofNullable(command.getPrice()).ifPresent(event::setPrice);
        Optional.ofNullable(command.getComment()).ifPresent(event::setComment);
        Optional.ofNullable(command.getDecorationDescription()).ifPresent(event::setDecorationDescription);
        return mapToDTO(event);
    }

    /** Deleting an event also removes the offer it came from — that is the only way a signed offer can be deleted. */
    @Transactional
    public void delete(int id) {
        Event event = getOwnedEvent(id);
        Integer offerId = event.getOfferId();
        eventRepository.delete(event);
        if (offerId != null) {
            offerRepository.findById(offerId).ifPresent(offerRepository::delete);
        }
    }

    public EventDTO createFromOffer(Offer offer) {
        Event event = eventRepository.save(fromOffer(offer));

        // Snapshot the offer's priced elements onto the event so both can evolve independently.
        List<EventElement> copies = eventElementRepository.findAllByOfferId(offer.getId()).stream()
                .map(source -> EventElement.builder()
                        .event(event)
                        .name(source.getName())
                        .quantity(source.getQuantity())
                        .unitPrice(source.getUnitPrice())
                        .build())
                .toList();
        if (!copies.isEmpty()) {
            eventElementRepository.saveAll(copies);
        }

        return mapToDTO(event);
    }

}
