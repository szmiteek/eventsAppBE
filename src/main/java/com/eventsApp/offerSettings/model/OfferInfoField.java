package com.eventsApp.offerSettings.model;

import com.eventsApp.offer.model.Offer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Client form fields a tenant can print on the "Informacje ogólne" page of the offer PDF.
 * Declaration order follows the client form and is how settings list them; the PDF uses the tenant's own arrangement.
 */
public enum OfferInfoField {
    PERSONAL_DATA("Imię i nazwisko", Offer::getPersonalData),
    EMAIL("E-mail", Offer::getEmail),
    PHONE("Telefon", Offer::getPhone),
    VENUE("Miejsce", Offer::getVenue),
    EVENT_DATE("Data eventu", offer -> formatDate(offer.getEventDate())),
    BUDGET("Orientacyjny budżet", offer -> offer.getBudget() != null ? offer.getBudget() + " zł" : null),
    GUESTS("Liczba gości", offer -> offer.getGuests() != null ? String.valueOf(offer.getGuests()) : null),
    EVENT_TYPE("Rodzaj wydarzenia", offer -> join(offer.getEventType())),
    DECORATION_TYPE("Rodzaj kompozycji", offer -> join(offer.getDecorationType())),
    MAIN_TABLE_TYPE("Typ stołu prezydialnego", Offer::getMainTableType),
    MAIN_TABLE_SEATS("Przy stole prezydialnym", Offer::getMainTableSeats),
    GUESTS_TABLE_TYPE("Typ stołów gości", Offer::getGuestsTableType),
    FLOWERS_TYPE("Rodzaj kwiatów", Offer::getFlowersType),
    COLORS("Kolorystyka", Offer::getColors),
    DESCRIPTION("Opis / dodatkowe informacje", Offer::getDescription);

    private final String label;
    private final Function<Offer, String> extractor;

    OfferInfoField(String label, Function<Offer, String> extractor) {
        this.label = label;
        this.extractor = extractor;
    }

    public String getLabel() {
        return label;
    }

    /** The offer's value as printed in the PDF, or null when the client left it empty. */
    public String extract(Offer offer) {
        return extractor.apply(offer);
    }

    private static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : null;
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(", ", values);
    }
}
