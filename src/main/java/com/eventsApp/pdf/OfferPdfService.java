package com.eventsApp.pdf;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.eventElement.EventElementRepository;
import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offer.OfferRepository;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offer.model.Offer;
import com.eventsApp.offerImage.OfferImageRepository;
import com.eventsApp.offerImage.model.OfferImage;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferPdfService {

    private static final String TEMPLATE_PATH = "/pdf/oferta-template.pdf";
    private static final String FONT_PATH = "/fonts/Inter.ttf";
    private static final int INFO_PAGE_INDEX = 2;
    private static final int PRICING_PAGE_INDEX = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int MAX_IMAGES = 4;

    private final OfferRepository offerRepository;
    private final OfferImageRepository offerImageRepository;
    private final EventElementRepository eventElementRepository;
    private final CurrentTenantProvider currentTenantProvider;
    private final OfferPdfStorageService offerPdfStorageService;

    public byte[] generateOfferPdf(int offerId, OfferPdfOverrides overrides) {
        int tenantId = currentTenantProvider.requireTenantId();
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(tenantId)) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        List<OfferImage> images = offerImageRepository.findByOfferId(offerId);

        LocalDate date = overrides.date() != null ? overrides.date() : offer.getEventDate();
        String venue = overrides.venue() != null ? overrides.venue() : offer.getVenue();
        Integer guests = overrides.guests() != null ? overrides.guests() : offer.getGuests();
        String colors = overrides.colors() != null ? overrides.colors() : offer.getColors();
        String mainTable = overrides.mainTable() != null ? overrides.mainTable() : offer.getMainTableType();
        String guestsTable = overrides.guestsTable() != null ? overrides.guestsTable() : offer.getGuestsTableType();
        String flowers = overrides.flowers();
        String decorationDescription = overrides.description() != null
                ? overrides.description()
                : offer.getDecorationDescription();

        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                throw new IllegalStateException("PDF template not found on classpath: " + TEMPLATE_PATH);
            }
            try (PDDocument document = Loader.loadPDF(templateStream.readAllBytes())) {
                PDFont font = loadFont(document);
                PDPage page = document.getPage(INFO_PAGE_INDEX);

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    drawValue(cs, font, date != null ? date.format(DATE_FORMAT) : "-", 81f, 554f);
                    drawValue(cs, font, venue, 408.3f, 541f);
                    drawValue(cs, font, guests != null ? String.valueOf(guests) : "-", 735.5f, 554f);
                    drawValue(cs, font, colors, 1062.7f, 554f);
                    drawValue(cs, font, mainTable, 65.9f, 336f);
                    drawValue(cs, font, guestsTable, 393.2f, 336f);
                    drawValue(cs, font, flowers, 735.5f, 336f);
                    drawImages(document, cs, images);
                }

                PDPage pricingPage = document.getPage(PRICING_PAGE_INDEX);
                try (PDPageContentStream cs = new PDPageContentStream(
                        document, pricingPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    drawPricingTable(cs, font, eventElementRepository.findAllByOfferId(offerId));
                    drawDescription(cs, font, decorationDescription);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.save(out);
                byte[] pdf = out.toByteArray();

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
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate offer PDF", e);
        }
    }

    public Optional<byte[]> loadSavedPdf(int offerId) {
        int tenantId = currentTenantProvider.requireTenantId();
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EventApiException("Offer not found", HttpStatus.NOT_FOUND));
        if (!offer.getTenantId().equals(tenantId)) {
            throw new EventApiException("Offer not found", HttpStatus.NOT_FOUND);
        }
        return offerPdfStorageService.load(tenantId, offerId);
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        try (InputStream fontStream = getClass().getResourceAsStream(FONT_PATH)) {
            if (fontStream == null) {
                throw new IllegalStateException("Font not found on classpath: " + FONT_PATH);
            }
            return PDType0Font.load(document, fontStream, true);
        }
    }

    private void drawValue(PDPageContentStream cs, PDFont font, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, 18);
        cs.setNonStrokingColor(new Color(64, 64, 64));
        cs.newLineAtOffset(x, y);
        cs.showText(text == null || text.isBlank() ? "-" : text);
        cs.endText();
    }

    private void drawImages(PDDocument document, PDPageContentStream cs, List<OfferImage> images) throws IOException {
        List<OfferImage> selected = images.stream().limit(MAX_IMAGES).toList();
        if (selected.isEmpty()) {
            return;
        }

        // Aligned with the divider line above "Nadesłane inspiracje" (x0≈230, right edge≈1180),
        // stretching from just below that line down to the page's bottom margin.
        float areaLeft = 230f;
        float areaRight = 1180f;
        float areaTopY = 252f;
        float areaBottomY = 40f;
        float gap = 25f;

        float boxWidth = (areaRight - areaLeft - gap * (MAX_IMAGES - 1)) / MAX_IMAGES;
        float boxHeight = areaTopY - areaBottomY;

        float x = areaLeft;
        for (OfferImage image : selected) {
            BufferedImage bufferedImage;
            try {
                bufferedImage = ImageIO.read(new ByteArrayInputStream(image.getData()));
            } catch (IOException e) {
                bufferedImage = null;
            }
            if (bufferedImage == null) {
                continue;
            }
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

            float scale = Math.min(boxWidth / pdImage.getWidth(), boxHeight / pdImage.getHeight());
            float drawW = pdImage.getWidth() * scale;
            float drawH = pdImage.getHeight() * scale;
            float drawX = x + (boxWidth - drawW) / 2;
            float drawY = areaBottomY + (boxHeight - drawH) / 2;

            cs.drawImage(pdImage, drawX, drawY, drawW, drawH);
            x += boxWidth + gap;
        }
    }

    private static final float TABLE_LEFT = 60f;
    // The template's own title divider sits at ~y=667; start well below it.
    private static final float TABLE_TOP = 620f;
    private static final float TABLE_BOTTOM = 140f;
    private static final float COL_NAME = 280f;
    private static final float COL_QTY = 100f;
    private static final float COL_UNIT_PRICE = 130f;
    private static final float COL_SUM = 130f;
    private static final float ROW_HEIGHT = 32f;
    private static final float NAME_LINE_HEIGHT = 16f;
    private static final float NAME_ROW_PADDING = 12f;
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");

    private void drawPricingTable(PDPageContentStream cs, PDFont font, List<EventElement> items) throws IOException {
        List<EventElement> rows = items != null ? items : List.of();
        float tableWidth = COL_NAME + COL_QTY + COL_UNIT_PRICE + COL_SUM;
        float y = TABLE_TOP;

        drawTableHeaderRow(cs, font, y);
        y -= ROW_HEIGHT;

        BigDecimal total = BigDecimal.ZERO;
        for (EventElement item : rows) {
            if (y < TABLE_BOTTOM) {
                break;
            }
            BigDecimal sum = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            List<String> nameLines = wrapText(font, 13, item.getName() == null || item.getName().isBlank() ? "-" : item.getName(), COL_NAME - 10);
            float rowHeight = Math.max(ROW_HEIGHT, nameLines.size() * NAME_LINE_HEIGHT + NAME_ROW_PADDING);
            drawTableRow(cs, font, y,
                    nameLines, String.valueOf(item.getQuantity()),
                    formatPrice(item.getUnitPrice()), formatPrice(sum));
            total = total.add(sum);
            y -= rowHeight;
        }

        cs.setStrokingColor(new Color(200, 200, 200));
        cs.setLineWidth(0.75f);
        cs.moveTo(TABLE_LEFT, y);
        cs.lineTo(TABLE_LEFT + tableWidth, y);
        cs.stroke();
        y -= ROW_HEIGHT * 0.7f;

        cs.beginText();
        cs.setFont(font, 15);
        cs.setNonStrokingColor(new Color(40, 40, 40));
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY, y);
        cs.showText("Razem:");
        cs.endText();

        cs.beginText();
        cs.setFont(font, 15);
        cs.setNonStrokingColor(new Color(40, 40, 40));
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY + COL_UNIT_PRICE, y);
        cs.showText(formatPrice(total));
        cs.endText();
    }

    private void drawTableHeaderRow(PDPageContentStream cs, PDFont font, float y) throws IOException {
        cs.setNonStrokingColor(new Color(120, 120, 120));
        cs.beginText();
        cs.setFont(font, 12);
        cs.newLineAtOffset(TABLE_LEFT, y);
        cs.showText("NAZWA");
        cs.endText();
        cs.beginText();
        cs.setFont(font, 12);
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME, y);
        cs.showText("ILOŚĆ SZT.");
        cs.endText();
        cs.beginText();
        cs.setFont(font, 12);
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY, y);
        cs.showText("CENA ZA SZT.");
        cs.endText();
        cs.beginText();
        cs.setFont(font, 12);
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY + COL_UNIT_PRICE, y);
        cs.showText("SUMA");
        cs.endText();

        cs.setStrokingColor(new Color(200, 200, 200));
        cs.setLineWidth(0.75f);
        cs.moveTo(TABLE_LEFT, y - 8f);
        cs.lineTo(TABLE_LEFT + COL_NAME + COL_QTY + COL_UNIT_PRICE + COL_SUM, y - 8f);
        cs.stroke();
    }

    private void drawTableRow(PDPageContentStream cs, PDFont font, float y,
                               List<String> nameLines, String quantity, String unitPrice, String sum) throws IOException {
        cs.setNonStrokingColor(new Color(64, 64, 64));
        cs.setFont(font, 13);

        float nameY = y;
        for (String line : nameLines) {
            cs.beginText();
            cs.newLineAtOffset(TABLE_LEFT, nameY);
            cs.showText(line);
            cs.endText();
            nameY -= NAME_LINE_HEIGHT;
        }

        cs.beginText();
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME, y);
        cs.showText(quantity);
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY, y);
        cs.showText(unitPrice);
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(TABLE_LEFT + COL_NAME + COL_QTY + COL_UNIT_PRICE, y);
        cs.showText(sum);
        cs.endText();
    }

    private String formatPrice(BigDecimal value) {
        return value != null ? PRICE_FORMAT.format(value) : "0.00";
    }

    private static final float DESC_LEFT = 800f;
    private static final float DESC_RIGHT = 1380f;
    private static final float DESC_TOP = 620f;
    private static final float DESC_BOTTOM = 140f;
    private static final float DESC_LINE_HEIGHT = 20f;

    private void drawDescription(PDPageContentStream cs, PDFont font, String description) throws IOException {
        if (description == null || description.isBlank()) {
            return;
        }
        float maxWidth = DESC_RIGHT - DESC_LEFT;
        float fontSize = 13;
        float y = DESC_TOP;

        for (String paragraph : description.split("\n")) {
            for (String line : wrapText(font, fontSize, paragraph, maxWidth)) {
                if (y < DESC_BOTTOM) {
                    return;
                }
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.setNonStrokingColor(new Color(64, 64, 64));
                cs.newLineAtOffset(DESC_LEFT, y);
                cs.showText(line);
                cs.endText();
                y -= DESC_LINE_HEIGHT;
            }
        }
    }

    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.getStringWidth(candidate) / 1000 * fontSize <= maxWidth || currentLine.isEmpty()) {
                currentLine = new StringBuilder(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

}
