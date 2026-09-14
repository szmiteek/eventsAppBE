package com.eventsApp.pdf;

import com.eventsApp.offerSettings.model.OfferInfoField;

/** A field of the "Informacje ogólne" page, pre-filled from the offer for editing in the PDF modal. */
public record OfferPdfFieldDTO(OfferInfoField key, String label, String value) {
}
