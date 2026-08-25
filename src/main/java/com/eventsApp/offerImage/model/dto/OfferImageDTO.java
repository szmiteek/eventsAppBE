package com.eventsApp.offerImage.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfferImageDTO {
    private Long id;
    private int offerId;
    private String filename;
    private String contentType;
}
