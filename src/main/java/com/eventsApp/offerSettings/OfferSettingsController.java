package com.eventsApp.offerSettings;

import com.eventsApp.offerSettings.model.TenantOfferSettings;
import com.eventsApp.offerSettings.model.command.OfferSettingsUpdateCommand;
import com.eventsApp.offerSettings.model.dto.OfferSettingsDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/offer-settings")
public class OfferSettingsController {

    private final OfferSettingsService offerSettingsService;

    @GetMapping
    public ResponseEntity<OfferSettingsDTO> get() {
        return ResponseEntity.ok(offerSettingsService.getOwn());
    }

    @PutMapping
    public ResponseEntity<OfferSettingsDTO> update(@Valid @RequestBody OfferSettingsUpdateCommand command) {
        return ResponseEntity.ok(offerSettingsService.update(command));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OfferSettingsDTO> uploadLogo(@RequestParam("logo") MultipartFile logo) {
        return ResponseEntity.ok(offerSettingsService.uploadLogo(logo));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        TenantOfferSettings settings = offerSettingsService.getOwnLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(settings.getLogoContentType()))
                .body(settings.getLogoData());
    }

    @DeleteMapping("/logo")
    public ResponseEntity<OfferSettingsDTO> deleteLogo() {
        return ResponseEntity.ok(offerSettingsService.deleteLogo());
    }

    @PostMapping(value = "/cover-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OfferSettingsDTO> uploadCoverPdf(@RequestParam("coverPdf") MultipartFile coverPdf) {
        return ResponseEntity.ok(offerSettingsService.uploadCoverPdf(coverPdf));
    }

    @GetMapping("/cover-pdf")
    public ResponseEntity<byte[]> getCoverPdf() {
        OfferSettingsService.CoverPdf coverPdf = offerSettingsService.getOwnCoverPdf();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + coverPdf.filename() + "\"")
                .body(coverPdf.data());
    }

    @DeleteMapping("/cover-pdf")
    public ResponseEntity<OfferSettingsDTO> deleteCoverPdf() {
        return ResponseEntity.ok(offerSettingsService.deleteCoverPdf());
    }
}
