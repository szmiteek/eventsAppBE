package com.eventsApp.offer;

import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.command.OfferUpdateStatusCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.eventsApp.offer.OfferMapper.fromCreateCommand;
import static com.eventsApp.offer.OfferMapper.mapToDTO;
import static com.eventsApp.offer.OfferMapper.updateFromCommand;

@Service
@AllArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;


    public Page<OfferDTO> getAll(Pageable pageable) {
        return offerRepository.findAll(pageable).map(OfferMapper::mapToDTO);
    }

    public OfferDTO createOffer(OfferCreateCommand offerRequest) {
        Offer offer = fromCreateCommand(offerRequest);
        offer.setCreatedDate(LocalDate.now());
        offer.setStatus(OfferStatus.NOT_SENT);
        return mapToDTO(offerRepository.save(offer));
    }

    public void deleteOffer(int id) {
        offerRepository.deleteById(id);
    }

    public OfferDTO getById(int id) {
        return offerRepository.findById(id)
                .map(OfferMapper::mapToDTO)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public OfferDTO update(int id, OfferUpdateCommand command) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (offer.getStatus() == OfferStatus.SIGNED) {
            throw new EventApiException("Signed offer can not be updated", HttpStatus.CONFLICT);
        }
        updateFromCommand(offer, command);
        return mapToDTO(offer);
    }

    @Transactional
    public OfferDTO updateStatus(int id, OfferUpdateStatusCommand command) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (offer.getStatus() == OfferStatus.SIGNED) {
            throw new EventApiException("Signed offer can not be updated", HttpStatus.CONFLICT);
        }
        offer.setStatus(command.getStatus());
        return mapToDTO(offer);
    }
}
