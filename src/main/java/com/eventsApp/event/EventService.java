package com.eventsApp.event;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.event.model.Event;
import com.eventsApp.event.model.command.EventCreateCommand;
import com.eventsApp.event.model.dto.EventDTO;
import com.eventsApp.event.model.dto.EventFilter;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.model.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.eventsApp.event.EventMapper.fromEventCreateCommand;
import static com.eventsApp.event.EventMapper.fromOffer;
import static com.eventsApp.event.EventMapper.mapToDTO;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public Page<EventDTO> getAll(Pageable pageable, EventFilter filters) {
        Specification<Event> spec = EventSpecification.build(filters, currentTenantProvider.requireTenantId());
        return eventRepository.findAll(spec, pageable).map(EventMapper::mapToDTO);
    }

    public EventDTO getById(int id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event not foud", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(event.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Event not foud", HttpStatus.NOT_FOUND);
        }
        return mapToDTO(event);
    }

    public EventDTO create(EventCreateCommand command) {
        Event event = fromEventCreateCommand(command);
        event.setTenantId(currentTenantProvider.requireTenantId());
        return mapToDTO(eventRepository.save(event));
    }

    public EventDTO createFromOffer(Offer offer) {
        Event event = fromOffer(offer);
        return mapToDTO(eventRepository.save(event));
    }

}
