package com.eventsApp.offer;

import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.OfferCreateRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;


    public Page<Offer> getAll(int page, int size) {
        return offerRepository.findAll(PageRequest.of(page, size));
    }

    public void createOffer(OfferCreateRequest offerRequest) {
        Offer offer = OfferMapper.toEntity(offerRequest);
        offer.setCreateDate(LocalDate.now());
        offer.setStatus(OfferStatus.NOT_SENT);
        offerRepository.save(offer);
    }

    public void deleteOffer(int id) {
        offerRepository.deleteById(id);
    }
}
