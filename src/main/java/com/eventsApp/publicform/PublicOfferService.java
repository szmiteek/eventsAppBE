package com.eventsApp.publicform;

import com.eventsApp.auth.TenantRepository;
import com.eventsApp.auth.model.Tenant;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offerImage.OfferImageRepository;
import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.publicform.model.command.PublicOfferCommand;
import com.eventsApp.publicform.model.dto.PublicTenantInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicOfferService {

    private final TenantRepository tenantRepository;
    private final OfferRepository offerRepository;
    private final OfferImageRepository offerImageRepository;
    private final PublicOfferRateLimiter rateLimiter;

    public PublicTenantInfoDTO getTenantInfo(String token) {
        Tenant tenant = findActiveTenant(token);
        return PublicTenantInfoDTO.builder()
                .companyName(tenant.getCompanyName())
                .build();
    }

    public void submit(String token, PublicOfferCommand command, List<MultipartFile> images) {
        if (command.getHoneypot() != null && !command.getHoneypot().isBlank()) {
            return;
        }
        if (!rateLimiter.allow(token)) {
            throw new EventApiException("Zbyt wiele zgłoszeń, spróbuj ponownie później", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (images == null || images.stream().allMatch(MultipartFile::isEmpty)) {
            throw new EventApiException("Dodaj co najmniej jedno zdjęcie", HttpStatus.BAD_REQUEST);
        }

        Tenant tenant = findActiveTenant(token);

        Offer offer = Offer.builder()
                .tenantId(tenant.getId())
                .personalData(command.getPersonalData())
                .venue(command.getVenue())
                .eventDate(command.getEventDate())
                .email(command.getEmail())
                .phone(command.getPhone())
                .budget(command.getBudget())
                .guests(command.getGuests())
                .eventType(command.getEventType())
                .decorationType(command.getDecorationType())
                .colors(command.getColors())
                .description(command.getDescription())
                .mainTableType(command.getMainTableType())
                .mainTableSeats(command.getMainTableSeats())
                .guestsTableType(command.getGuestsTableType())
                .flowersType(command.getFlowersType())
                .status(OfferStatus.NOT_READY)
                .createdDate(LocalDate.now())
                .build();
        Offer saved = offerRepository.save(offer);

        if (images != null && !images.isEmpty()) {
            List<OfferImage> offerImages = images.stream()
                    .filter(file -> !file.isEmpty())
                    .map(file -> toOfferImage(saved, file))
                    .toList();
            offerImageRepository.saveAll(offerImages);
        }
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

    private Tenant findActiveTenant(String token) {
        return tenantRepository.findByPublicFormToken(token)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new EventApiException("Not found", HttpStatus.NOT_FOUND));
    }
}
