package com.eventsApp.offer;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.event.EventService;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.command.OfferUpdateStatusCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;
import com.eventsApp.offer.model.dto.OfferFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.eventsApp.offer.OfferMapper.fromCreateCommand;
import static com.eventsApp.offer.OfferMapper.mapToDTO;
import static com.eventsApp.offer.OfferMapper.updateFromCommand;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;
    private final EventService eventService;
    private final CurrentTenantProvider currentTenantProvider;

    public Page<OfferDTO> getAll(Pageable pageable, OfferFilter filters) {
        Specification<Offer> spec = OfferSpecification.build(filters, currentTenantProvider.requireTenantId());
        return offerRepository.findAll(spec, pageable).map(OfferMapper::mapToDTO);
    }

    public OfferDTO create(OfferCreateCommand offerRequest) {
        Offer offer = fromCreateCommand(offerRequest);
        offer.setTenantId(currentTenantProvider.requireTenantId());
        offer.setCreatedDate(LocalDate.now());
        offer.setStatus(OfferStatus.NOT_READY);
        return mapToDTO(offerRepository.save(offer));
    }

    public void delete(int id) {
        Offer offer = getOwnedOffer(id);
        offerRepository.delete(offer);
    }

    public OfferDTO getById(int id) {
        return mapToDTO(getOwnedOffer(id));
    }

    @Transactional
    public OfferDTO update(int id, OfferUpdateCommand command) {
        Offer offer = getOwnedOffer(id);
        if (offer.getStatus() == OfferStatus.SIGNED) {
            throw new EventApiException("Signed offer can not be updated", HttpStatus.CONFLICT);
        }
        updateFromCommand(offer, command);
        return mapToDTO(offer);
    }

    @Transactional
    public OfferDTO updateStatus(int id, OfferUpdateStatusCommand command) {
        Offer offer = getOwnedOffer(id);
        if (offer.getStatus() == OfferStatus.SIGNED) {
            throw new EventApiException("Signed offer can not be updated", HttpStatus.CONFLICT);
        }
        if (command.getStatus() == OfferStatus.SIGNED) {
            eventService.createFromOffer(offer);
        }
        offer.setStatus(command.getStatus());
        return mapToDTO(offer);
    }

    private Offer getOwnedOffer(int id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        return offer;
    }
}
