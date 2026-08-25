package com.eventsApp.offer;

import com.eventsApp.offer.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface OfferRepository extends JpaRepository<Offer, Integer>, JpaSpecificationExecutor<Offer> {
}
