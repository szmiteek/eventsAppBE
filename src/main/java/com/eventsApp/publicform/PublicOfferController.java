package com.eventsApp.publicform;

import com.eventsApp.publicform.model.command.PublicOfferCommand;
import com.eventsApp.publicform.model.dto.PublicTenantInfoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/public/offers")
public class PublicOfferController {

    private final PublicOfferService publicOfferService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicTenantInfoDTO> getTenantInfo(@PathVariable String token) {
        return ResponseEntity.ok(publicOfferService.getTenantInfo(token));
    }

    @PostMapping(value = "/{token}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> submit(@PathVariable String token,
                                        @Valid @ModelAttribute PublicOfferCommand command,
                                        @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        publicOfferService.submit(token, command, images);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
