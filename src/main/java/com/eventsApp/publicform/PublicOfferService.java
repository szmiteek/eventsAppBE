package com.eventsApp.publicform;

import com.eventsApp.auth.TenantRepository;
import com.eventsApp.auth.model.Tenant;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offerImage.OfferImageRepository;
import com.eventsApp.offerImage.OfferImageService;
import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.offerSettings.OfferSettingsService;
import com.eventsApp.offerSettings.model.TenantOfferSettings;
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
    private final OfferSettingsService offerSettingsService;
    private final PublicOfferRateLimiter rateLimiter;

    public PublicTenantInfoDTO getTenantInfo(String token) {
        Tenant tenant = findActiveTenant(token);
        return PublicTenantInfoDTO.builder()
                .companyName(tenant.getCompanyName())
                .hasLogo(hasLogo(offerSettingsService.resolveForTenant(tenant.getId())))
                .build();
    }

    /** The logo shown on the tenant's public form — served without auth, like the form itself. */
    public TenantOfferSettings getLogo(String token) {
        Tenant tenant = findActiveTenant(token);
        TenantOfferSettings settings = offerSettingsService.resolveForTenant(tenant.getId());
        if (!hasLogo(settings)) {
            throw new EventApiException("Not found", HttpStatus.NOT_FOUND);
        }
        return settings;
    }

    public void submit(String token, PublicOfferCommand command, List<MultipartFile> images) {
        if (command.getHoneypot() != null && !command.getHoneypot().isBlank()) {
            return;
        }
        if (!rateLimiter.allow(token)) {
            throw new EventApiException("Zbyt wiele zgłoszeń, spróbuj ponownie później", HttpStatus.TOO_MANY_REQUESTS);
        }

        long selectedImages = images == null ? 0 : images.stream().filter(file -> !file.isEmpty()).count();
        if (selectedImages == 0) {
            throw new EventApiException("Dodaj co najmniej jedno zdjęcie", HttpStatus.BAD_REQUEST);
        }
        if (selectedImages > OfferImageService.MAX_IMAGES_PER_OFFER) {
            throw new EventApiException(
                    "Możesz dodać maksymalnie " + OfferImageService.MAX_IMAGES_PER_OFFER + " zdjęć",
                    HttpStatus.BAD_REQUEST);
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
                .appetizersOnTable(Boolean.TRUE.equals(command.getAppetizersOnTable()))
                .status(OfferStatus.NOT_READY)
                .createdDate(LocalDate.now())
                .build();
        Offer saved = offerRepository.save(offer);

        List<OfferImage> offerImages = images.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> toOfferImage(saved, file))
                .toList();
        offerImageRepository.saveAll(offerImages);
    }

    private boolean hasLogo(TenantOfferSettings settings) {
        return settings.getLogoData() != null && settings.getLogoData().length > 0;
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
