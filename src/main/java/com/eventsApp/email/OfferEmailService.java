package com.eventsApp.email;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.auth.TenantRepository;
import com.eventsApp.auth.model.Tenant;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.integration.mail.MailMessage;
import com.eventsApp.integration.mail.TenantMailSender;
import com.eventsApp.offer.OfferMapper;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.dto.OfferDTO;
import com.eventsApp.pdf.OfferPdfStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferEmailService {

    private final OfferRepository offerRepository;
    private final TenantRepository tenantRepository;
    private final CurrentTenantProvider currentTenantProvider;
    private final OfferPdfStorageService offerPdfStorageService;
    private final TenantMailSender tenantMailSender;

    public OfferDTO sendOfferEmail(int offerId, String emailOverride) {
        int tenantId = currentTenantProvider.requireTenantId();

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(tenantId)) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        // SENT is allowed too — a regenerated offer can be sent to the client again.
        if (offer.getStatus() != OfferStatus.READY && offer.getStatus() != OfferStatus.SENT) {
            throw new EventApiException(
                    "Ofertę można wysłać dopiero po przygotowaniu PDF-a.", HttpStatus.CONFLICT);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EventApiException("Tenant not found", HttpStatus.NOT_FOUND));

        byte[] pdf = offerPdfStorageService.load(tenantId, offerId)
                .orElseThrow(() -> new EventApiException(
                        "Brak wygenerowanego PDF-a dla tej oferty. Przygotuj ofertę ponownie.", HttpStatus.CONFLICT));

        String recipient = emailOverride != null && !emailOverride.isBlank()
                ? emailOverride.trim()
                : offer.getEmail();
        if (recipient == null || recipient.isBlank()) {
            throw new EventApiException("Oferta nie ma adresu e-mail klienta.", HttpStatus.BAD_REQUEST);
        }

        tenantMailSender.send(new MailMessage(
                recipient,
                "Oferta – " + tenant.getCompanyName(),
                tenant.getEmailMessage(),
                "oferta-" + offer.getId() + ".pdf",
                pdf));

        // A corrected address is kept on the offer so the typo does not come back next time.
        offer.setEmail(recipient);
        offer.setStatus(OfferStatus.SENT);
        offerRepository.save(offer);
        return OfferMapper.mapToDTO(offer);
    }
}
