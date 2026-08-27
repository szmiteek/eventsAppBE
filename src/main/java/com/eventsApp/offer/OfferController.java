package com.eventsApp.offer;


import com.eventsApp.email.OfferEmailService;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.model.command.OfferSendCommand;
import com.eventsApp.offer.model.command.OfferUpdateCommand;
import com.eventsApp.offer.model.command.OfferUpdateStatusCommand;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.offer.model.command.OfferCreateCommand;
import com.eventsApp.offer.model.dto.OfferFilter;
import com.eventsApp.offerImage.OfferImageService;
import com.eventsApp.offerImage.model.dto.OfferImageDTO;
import com.eventsApp.pdf.OfferPdfOverrides;
import com.eventsApp.pdf.OfferPdfService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/offer")
public class OfferController {

    private final OfferService offerService;
    private final OfferImageService offerImageService;
    private final OfferPdfService offerPdfService;
    private final OfferEmailService offerEmailService;

    @GetMapping()
    public ResponseEntity<Page<OfferDTO>> getAll(@PageableDefault Pageable pageable, OfferFilter filters) {
        return new ResponseEntity(offerService.getAll(pageable, filters), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDTO> getById(@PathVariable int id) {
        return new ResponseEntity(offerService.getById(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable int id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String colors,
            @RequestParam(required = false) String mainTable,
            @RequestParam(required = false) String guestsTable,
            @RequestParam(required = false) String flowers,
            @RequestParam(required = false) String description) {
        byte[] pdf = offerPdfService.generateOfferPdf(id,
                new OfferPdfOverrides(date, venue, guests, colors, mainTable, guestsTable, flowers, description));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"oferta-" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> downloadSavedPdf(@PathVariable int id) {
        byte[] pdf = offerPdfService.loadSavedPdf(id)
                .orElseThrow(() -> new EventApiException("PDF not generated yet for this offer", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"oferta-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<OfferDTO> sendEmail(@PathVariable int id,
                                              @Valid @RequestBody(required = false) OfferSendCommand command) {
        return ResponseEntity.ok(
                offerEmailService.sendOfferEmail(id, command != null ? command.getEmail() : null));
    }

    @PostMapping()
    public ResponseEntity<OfferDTO> create(@Valid @RequestBody OfferCreateCommand offer) {
        return new ResponseEntity(offerService.create(offer), HttpStatus.CREATED);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OfferDTO> createWithImages(
            @Valid @ModelAttribute OfferCreateCommand offer,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        OfferDTO created = offerService.create(offer);
        if (images != null && !images.isEmpty()) {
            offerImageService.saveAll(created.getId(), images);
        }
        return new ResponseEntity(created, HttpStatus.CREATED);
    }

    @PutMapping({"/{id}"})
    public ResponseEntity<OfferDTO> update(@PathVariable int id, @Valid @RequestBody OfferUpdateCommand command) {
        return new ResponseEntity(offerService.update(id, command), HttpStatus.OK);
    }

    @PatchMapping({"/{id}"})
    public ResponseEntity<OfferDTO> updateStatus(@PathVariable int id, @Valid @RequestBody OfferUpdateStatusCommand command) {
        return new ResponseEntity(offerService.updateStatus(id, command), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        offerService.delete(id);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
