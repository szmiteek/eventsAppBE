package com.eventsApp.offer;


import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.OfferCreateRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@AllArgsConstructor
@RequestMapping("/offers")
public class OfferController {

    private OfferService offerService;

    @GetMapping("")
    public ResponseEntity<Page<Offer>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok().body(offerService.getAll(page, size));
    }

    @PostMapping("")
    public void createOffer(@RequestBody OfferCreateRequest offer) {
        offerService.createOffer(offer);
    }

    @DeleteMapping("{id}")
    public void deleteOffer(@PathVariable int id) {
        offerService.deleteOffer(id);
    }
}
