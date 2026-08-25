package com.eventsApp.offerImage;

import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.offerImage.model.dto.OfferImageDTO;

public class OfferImageMapper {
    public static OfferImageDTO mapToDTO(OfferImage image) {
        return OfferImageDTO.builder()
                .id(image.getId())
                .offerId(image.getOffer().getId())
                .filename(image.getFilename())
                .contentType(image.getContentType())
                .build();
    }
}
