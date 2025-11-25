package com.eventsApp.offer;

import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.OfferCreateRequest;

public class OfferMapper {
    public static Offer toEntity(OfferCreateRequest dto) {
        Offer offer = new Offer();
        offer.setPersonalData(dto.getPersonalData());
        offer.setVenue(dto.getVenue());
        offer.setEventDate(dto.getEventDate());
        offer.setEmail(dto.getEmail());
        offer.setPhone(dto.getPhone());
        offer.setBudget(dto.getBudget());
        offer.setGuests(dto.getGuests());
        return offer;
    }
}
