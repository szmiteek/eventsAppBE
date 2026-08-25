package com.eventsApp.eventElement;

import com.eventsApp.auth.CurrentTenantProvider;
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
    private final CurrentTenantProvider currentTenantProvider;

    public EventElementDTO create(EventElementCreateCommand command) {
        Offer offer = getOwnedOffer(command.getOfferId());

        EventElement element = fromCreateCommand(command);
        element.setOffer(offer);
        EventElement saved = eventElementRepository.save(element);
        recalculateOfferPrice(offer.getId());
        return mapToDTO(saved);
    }

    public EventElementDTO update(int id, EventElementUpdateCommand command) {
        EventElement element = eventElementRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event element not found", HttpStatus.NOT_FOUND));
        getOwnedOffer(element.getOffer().getId());
        updateFromCommand(element, command);
        EventElement saved = eventElementRepository.save(element);
        recalculateOfferPrice(saved.getOffer().getId());
        return mapToDTO(saved);
    }

    public void delete(int id) {
        EventElement element = eventElementRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event element not found", HttpStatus.NOT_FOUND));
        int offerId = element.getOffer().getId();
        getOwnedOffer(offerId);
        eventElementRepository.deleteById(id);
        recalculateOfferPrice(offerId);
    }

    public List<EventElementDTO> getAllByOfferId(int offerId) {
        getOwnedOffer(offerId);
        return eventElementRepository.findAllByOfferId(offerId).stream()
                .map(EventElementMapper::mapToDTO)
                .toList();
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
