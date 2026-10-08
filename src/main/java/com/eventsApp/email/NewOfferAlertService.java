package com.eventsApp.email;

import com.eventsApp.auth.model.Tenant;
import com.eventsApp.integration.mail.SystemMailSender;
import com.eventsApp.offer.model.Offer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/** Powiadomienie dla tenanta o zapytaniu, które właśnie wpadło z formularza klienta. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewOfferAlertService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SystemMailSender systemMailSender;

    /**
     * Alert nigdy nie może przewrócić zgłoszenia ani go opóźnić: oferta jest już zapisana, a klient po
     * drugiej stronie formularza nie ma nic wspólnego z tym, że serwer pocztowy akurat wolno odpowiada.
     * Treść składamy tutaj, na wątku żądania, i do wysyłki idą same napisy — encje JPA nie przechodzą
     * między wątkami, bo ich leniwe kolekcje bez sesji by się wywróciły.
     */
    public void sendNewOfferAlert(Tenant tenant, Offer offer) {
        if (!tenant.isAlertEmailEnabled()) {
            return;
        }
        String recipient = tenant.resolveAlertEmail();
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            systemMailSender.sendAsync(
                    recipient,
                    "Nowe zapytanie o ofertę – " + offer.getPersonalData(),
                    buildBody(offer));
        } catch (Exception e) {
            log.warn("Nie udało się zlecić alertu o ofercie {} dla tenanta {}: {}",
                    offer.getId(), tenant.getId(), e.getMessage());
        }
    }

    private String buildBody(Offer offer) {
        StringBuilder body = new StringBuilder("Przez formularz wpłynęło nowe zapytanie o ofertę.\n\n");
        append(body, "Klient", offer.getPersonalData());
        append(body, "E-mail", offer.getEmail());
        append(body, "Telefon", offer.getPhone());
        append(body, "Miejsce", offer.getVenue());
        append(body, "Data eventu", offer.getEventDate() != null ? offer.getEventDate().format(DATE) : null);
        append(body, "Liczba gości", offer.getGuests() != null ? String.valueOf(offer.getGuests()) : null);
        append(body, "Orientacyjny budżet", offer.getBudget() != null ? offer.getBudget() + " zł" : null);
        body.append("\nSzczegóły zapytania znajdziesz na liście ofert w aplikacji.");
        return body.toString();
    }

    private void append(StringBuilder body, String label, String value) {
        if (value != null && !value.isBlank()) {
            body.append(label).append(": ").append(value).append('\n');
        }
    }
}
