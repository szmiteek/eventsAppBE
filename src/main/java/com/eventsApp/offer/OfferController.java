package com.eventsApp.offer;


import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.command.OfferUpdateStatusCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
@RequestMapping("api/offers")
public class OfferController {

    private OfferService offerService;

    @GetMapping()
    public ResponseEntity<Page<OfferDTO>> getAll(@PageableDefault Pageable pageable) {
        return new ResponseEntity(offerService.getAll(pageable), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDTO> getById(@PathVariable int id) {
        return new ResponseEntity(offerService.getById(id), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<OfferDTO> createOffer(@RequestBody OfferCreateCommand offer) {
        return new ResponseEntity(offerService.createOffer(offer), HttpStatus.CREATED);
    }

    @PutMapping({"/{id}"})
    public ResponseEntity<OfferDTO> updateOffer(@PathVariable int id, @RequestBody OfferUpdateCommand command) {
        return new ResponseEntity(offerService.update(id, command), HttpStatus.OK);
    }

    @PatchMapping ({"/{id}"})
    public ResponseEntity<OfferDTO> updateStatus(@PathVariable int id, @RequestBody OfferUpdateStatusCommand command) {
        return new ResponseEntity(offerService.updateStatus(id, command), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable int id) {
        offerService.deleteOffer(id);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
