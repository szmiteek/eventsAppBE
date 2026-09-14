package com.eventsApp.pdf;

import com.eventsApp.offerSettings.model.OfferInfoField;

import java.util.Map;

/**
 * Values edited in the PDF modal, used for the generated file only. Fields left out fall back to the offer's
 * data; a missing decorationDescription keeps the offer's own.
 */
public record OfferPdfOverrides(Map<OfferInfoField, String> fields, String decorationDescription) {
}
