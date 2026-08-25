package com.eventsApp.pdf;

import java.time.LocalDate;

public record OfferPdfOverrides(
        LocalDate date,
        String venue,
        Integer guests,
        String colors,
        String mainTable,
        String guestsTable,
        String flowers,
        String description
) {
}
