package com.eventsApp.offerImage;

import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.offerImage.model.dto.OfferImageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/offer-image")
public class OfferImageController {

    private final OfferImageService offerImageService;

    @GetMapping("/offer/{offerId}")
    public ResponseEntity<List<OfferImageDTO>> getAllByOfferId(@PathVariable int offerId) {
        return ResponseEntity.ok(offerImageService.getByOfferId(offerId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<OfferImageDTO>> getAllByEventId(@PathVariable int eventId) {
        return ResponseEntity.ok(offerImageService.getByEventId(eventId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getContent(@PathVariable Long id) {
        OfferImage image = offerImageService.getRawById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(image.getData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offerImageService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
