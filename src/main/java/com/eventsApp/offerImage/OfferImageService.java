package com.eventsApp.offerImage;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.event.EventRepository;
import com.eventsApp.event.model.Event;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.offerImage.model.dto.OfferImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.eventsApp.offerImage.OfferImageMapper.mapToDTO;

@Service
@RequiredArgsConstructor
public class OfferImageService {
    private final OfferImageRepository offerImageRepository;
    private final OfferRepository offerRepository;
    private final EventRepository eventRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public List<OfferImageDTO> saveAll(int offerId, List<MultipartFile> files) {
        Offer offer = getOwnedOffer(offerId);

        List<OfferImage> images = files.stream()
                .map(file -> toOfferImage(offer, file))
                .toList();

        return offerImageRepository.saveAll(images).stream()
                .map(OfferImageMapper::mapToDTO)
                .toList();
    }

    public List<OfferImageDTO> getByOfferId(int offerId) {
        getOwnedOffer(offerId);
        return offerImageRepository.findByOfferId(offerId).stream()
                .map(OfferImageMapper::mapToDTO)
                .toList();
    }

    public List<OfferImageDTO> getByEventId(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventApiException("Event not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(event.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Event not found", HttpStatus.NOT_FOUND);
        }
        return getByOfferId(event.getOfferId());
    }

    public OfferImage getRawById(Long id) {
        OfferImage image = offerImageRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Image not found", HttpStatus.NOT_FOUND));
        getOwnedOffer(image.getOffer().getId());
        return image;
    }

    public void delete(Long id) {
        OfferImage image = offerImageRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Image not found", HttpStatus.NOT_FOUND));
        getOwnedOffer(image.getOffer().getId());
        offerImageRepository.deleteById(id);
    }

    private Offer getOwnedOffer(int offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        return offer;
    }

    private OfferImage toOfferImage(Offer offer, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new EventApiException("Only image files are allowed", HttpStatus.BAD_REQUEST);
        }
        try {
            return OfferImage.builder()
                    .offer(offer)
                    .filename(file.getOriginalFilename())
                    .contentType(contentType)
                    .data(file.getBytes())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded image", e);
        }
    }
}
