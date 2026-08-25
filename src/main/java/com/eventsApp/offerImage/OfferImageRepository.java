package com.eventsApp.offerImage;

import com.eventsApp.offerImage.model.OfferImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferImageRepository extends JpaRepository<OfferImage, Long> {
    List<OfferImage> findByOfferId(int offerId);
}
