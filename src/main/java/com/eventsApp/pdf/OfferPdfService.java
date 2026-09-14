package com.eventsApp.pdf;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.eventElement.EventElementRepository;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offerImage.OfferImageRepository;
import com.eventsApp.offerImage.model.OfferImage;
import com.eventsApp.offerSettings.OfferSettingsService;
import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.TenantOfferSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferPdfService {

    private final OfferRepository offerRepository;
    private final OfferImageRepository offerImageRepository;
    private final EventElementRepository eventElementRepository;
    private final CurrentTenantProvider currentTenantProvider;
    private final OfferPdfStorageService offerPdfStorageService;
    private final OfferSettingsService offerSettingsService;
    private final OfferPdfRenderer offerPdfRenderer;

    /** The fields the tenant chose for the "Informacje ogólne" page, pre-filled from the offer for the PDF modal. */
    public List<OfferPdfFieldDTO> getPdfFields(int offerId) {
        Offer offer = getOwnedOffer(offerId);
        TenantOfferSettings settings = offerSettingsService.resolveForTenant(offer.getTenantId());
        return settings.getInfoFields().stream()
                .map(field -> new OfferPdfFieldDTO(field, field.getLabel(), Objects.requireNonNullElse(field.extract(offer), "")))
                .toList();
    }

    public byte[] generateOfferPdf(int offerId, OfferPdfOverrides overrides) {
        Offer offer = getOwnedOffer(offerId);
        int tenantId = offer.getTenantId();
        TenantOfferSettings settings = offerSettingsService.resolveForTenant(tenantId);

        Map<OfferInfoField, String> fieldOverrides = overrides != null && overrides.fields() != null
                ? overrides.fields()
                : Map.of();
        String decorationDescription = overrides != null && overrides.decorationDescription() != null
                ? overrides.decorationDescription()
                : offer.getDecorationDescription();

        // Edited values from the modal win; fields the modal didn't send fall back to the offer's data.
        List<OfferPdfRenderer.Field> fields = settings.getInfoFields().stream()
                .map(field -> new OfferPdfRenderer.Field(field.getLabel(),
                        fieldOverrides.containsKey(field) ? fieldOverrides.get(field) : field.extract(offer)))
                .toList();
        List<byte[]> images = offerImageRepository.findByOfferId(offerId).stream()
                .limit(OfferPdfRenderer.MAX_IMAGES)
                .map(OfferImage::getData)
                .toList();

        byte[] pdf;
        try {
            pdf = offerPdfRenderer.render(new OfferPdfRenderer.Content(
                    settings.getBackgroundColor(),
                    settings.getOrientation(),
                    settings.getLogoData(),
                    fields,
                    images,
                    eventElementRepository.findAllByOfferIdOrderByPositionAscIdAsc(offerId),
                    decorationDescription));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate offer PDF", e);
        }

        offerPdfStorageService.save(tenantId, offerId, pdf);
        // The modal edits the offer's own description, so keep the two in sync (same as event elements).
        offer.setDecorationDescription(decorationDescription);
        offer.setPdfGeneratedDate(LocalDateTime.now());
        if (offer.getStatus() == OfferStatus.NOT_READY) {
            offer.setStatus(OfferStatus.READY);
        }
        offerRepository.save(offer);

        return pdf;
    }

    public Optional<byte[]> loadSavedPdf(int offerId) {
        Offer offer = getOwnedOffer(offerId);
        return offerPdfStorageService.load(offer.getTenantId(), offerId);
    }

    private Offer getOwnedOffer(int offerId) {
        int tenantId = currentTenantProvider.requireTenantId();
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(tenantId)) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        return offer;
    }
}
