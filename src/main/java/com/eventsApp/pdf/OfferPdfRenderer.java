package com.eventsApp.pdf;

import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.offerImage.OfferImageService;
import com.eventsApp.offerSettings.model.PdfOrientation;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lays out the offer PDF from scratch: page 1 "Informacje ogólne" (the tenant's chosen client fields and the
 * inspiration photos), page 2 "Wycena" (priced elements and the decoration description). Every page carries
 * the title on the left and the tenant's logo on the right, on the tenant's background colour.
 */
@Component
@Slf4j
public class OfferPdfRenderer {

    /** Tyle zdjęć, ile oferta może mieć — PDF nie pomija żadnego. */
    public static final int MAX_IMAGES = OfferImageService.MAX_IMAGES_PER_OFFER;

    /** Tytuł strony: Playfair Display (SIL OFL). */
    private static final String TITLE_FONT_PATH = "/fonts/PlayfairDisplay.ttf";
    /**
     * Cały pozostały tekst: Arimo (SIL OFL), darmowy odpowiednik Arial o identycznych metrykach. Arial MT Pro
     * jest komercyjny, a repozytorium publiczne, więc jego pliku tu nie wolno dodawać. Brakujący plik nie
     * wyłącza generowania PDF-ów: renderer zapisuje ostrzeżenie w logu i używa Intera.
     */
    private static final String BODY_FONT_PATH = "/fonts/Arimo.ttf";
    private static final String FALLBACK_FONT_PATH = "/fonts/Inter.ttf";
    private static final String INFO_TITLE = "Informacje ogólne";
    private static final String PRICING_TITLE = "Wycena";
    private static final String PRICING_CONTINUED_TITLE = "Wycena (cd.)";
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    private static final float MARGIN = 40f;
    private static final float HEADER_HEIGHT = 56f;
    private static final float HEADER_GAP = 24f;
    private static final float LOGO_MAX_WIDTH = 150f;

    /**
     * Strony z własnego PDF-a tenanta mają 1440×810 pt i właśnie w tym formacie ogląda się ofertę. Układ
     * liczymy w jednostkach A4, a na taką stronę skaluje go ten współczynnik (ogranicza go wysokość strony).
     */
    private static final float REFERENCE_SCALE = 810f / PDRectangle.A4.getWidth();
    /** Rozmiary czcionek podajemy tak, jak wyglądają na stronie 1440×810; na A4 skalują się proporcjonalnie. */
    private static final float TITLE_SIZE = onReferencePage(37f);
    private static final float TEXT_SIZE = onReferencePage(18f);
    private static final float LINE_HEIGHT = TEXT_SIZE * 1.35f;
    private static final float LABEL_GAP = 8f;
    /** Maksymalna wysokość rzędu zdjęć — bez limitu brały całą resztę strony i dominowały nad danymi. */
    private static final float IMAGE_ROW_MAX_HEIGHT = 230f;
    private static final int VALUE_MAX_LINES = 3;
    /** Od tej szerokości treści (jednostki układu) etykiety mieszczą się w czterech kolumnach. */
    private static final float WIDE_PAGE_CONTENT_WIDTH = 900f;
    private static final float CELL_GAP_X = 24f;
    private static final float CELL_GAP_Y = 20f;
    private static final float IMAGE_GAP = 8f;
    private static final int MAX_PHOTO_SIDE_PX = 1600;
    private static final int MAX_LOGO_SIDE_PX = 1000;

    private static final float TABLE_ROW_PADDING = 8f;
    private static final float TABLE_HEADER_HEIGHT = 24f;
    private static final float TABLE_TOTAL_HEIGHT = 38f;
    private static final float COL_QTY = 56f;
    private static final float COL_UNIT_PRICE = 115f;
    private static final float COL_SUM = 100f;
    private static final float COL_GAP = 10f;
    private static final float COLUMN_GAP = 32f;
    private static final float LANDSCAPE_TABLE_SHARE = 0.58f;

    public record Field(String label, String value) {
    }

    public record Content(String backgroundColor,
                          PdfOrientation orientation,
                          byte[] logo,
                          List<Field> fields,
                          List<byte[]> images,
                          List<EventElement> elements,
                          String decorationDescription) {
    }

    /**
     * Puts the generated pages at the end of the tenant's own PDF. Page sizes may differ between the two —
     * a PDF can mix them, so a portrait welcome page works with landscape offer pages.
     */
    public byte[] appendToCover(byte[] coverPdf, byte[] generatedPdf) throws IOException {
        try (PDDocument cover = Loader.loadPDF(coverPdf);
             PDDocument generated = Loader.loadPDF(generatedPdf)) {
            new PDFMergerUtility().appendDocument(cover, generated);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            cover.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Page size of a PDF's first page. The generated pages are rendered at this size when the tenant uploaded
     * their own PDF, so the whole file keeps one format instead of mixing A4 with, say, a 16:9 presentation.
     */
    public PDRectangle firstPageSize(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.getNumberOfPages() == 0) {
                return null;
            }
            PDPage page = document.getPage(0);
            PDRectangle box = page.getMediaBox();
            // A page can carry a /Rotate — the size a reader shows is then the box turned on its side.
            int rotation = (page.getRotation() % 360 + 360) % 360;
            return rotation == 90 || rotation == 270
                    ? new PDRectangle(box.getHeight(), box.getWidth())
                    : new PDRectangle(box.getWidth(), box.getHeight());
        }
    }

    public byte[] render(Content content) throws IOException {
        return render(content, null);
    }

    /** pageSize overrides the A4 size implied by the orientation setting; null keeps A4. */
    public byte[] render(Content content, PDRectangle pageSize) throws IOException {
        try (PDDocument document = new PDDocument()) {
            Layout layout = new Layout(document, content, pageSize);
            try {
                layout.drawInfoPage();
                layout.drawPricingPages();
            } finally {
                layout.closeStreams();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /** Per-document state: font, colours and the open page streams. */
    private final class Layout {

        private final PDDocument document;
        private final Content content;
        /** Cały tekst poza tytułem. */
        private final PDFont font;
        private final PDFont titleFont;
        private final PDRectangle pageSize;
        /** Page in layout units: the sizes below are A4-based and scaled onto the real page. */
        private final PDRectangle layoutSize;
        private final float scale;
        private final boolean portrait;
        private final Color background;
        private final Color text;
        private final Color muted;
        private final Color divider;
        private final PDImageXObject logo;
        private final List<PDPageContentStream> streams = new ArrayList<>();
        private final List<PageArea> pricingPages = new ArrayList<>();
        private final Map<Integer, Boolean> encodable = new HashMap<>();

        Layout(PDDocument document, Content content, PDRectangle pageSize) throws IOException {
            this.document = document;
            this.content = content;
            this.font = loadFont(document, BODY_FONT_PATH);
            this.titleFont = loadFont(document, TITLE_FONT_PATH);
            // An imposed size (the tenant's own PDF) wins over the orientation setting, and its own
            // proportions decide the layout — a 16:9 page is laid out like a landscape one.
            this.pageSize = pageSize != null
                    ? pageSize
                    : content.orientation() == PdfOrientation.PORTRAIT
                            ? PDRectangle.A4
                            : new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            this.portrait = this.pageSize.getHeight() > this.pageSize.getWidth();

            // Everything below is measured for an A4 page. On a bigger page those sizes would leave the content
            // as a thin strip at the top, so the whole drawing is scaled up instead — type, margins and table
            // grow proportionally, and the extra width of a wider format simply widens the columns.
            float a4Width = portrait ? PDRectangle.A4.getWidth() : PDRectangle.A4.getHeight();
            float a4Height = portrait ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth();
            this.scale = Math.min(this.pageSize.getWidth() / a4Width, this.pageSize.getHeight() / a4Height);
            this.layoutSize = new PDRectangle(this.pageSize.getWidth() / scale, this.pageSize.getHeight() / scale);
            this.background = parseColor(content.backgroundColor());
            this.text = readableTextColor(background);
            this.muted = mix(text, background, 0.4f);
            this.divider = mix(text, background, 0.82f);
            this.logo = content.logo() != null ? toImage(content.logo(), MAX_LOGO_SIDE_PX) : null;
        }

        // ---- page 1: Informacje ogólne ----

        void drawInfoPage() throws IOException {
            PageArea page = newPage(INFO_TITLE);
            float contentWidth = contentWidth();
            // Przy 18 pt etykiety typu „TYP STOŁU PREZYDIALNEGO” nie mieszczą się w czterech kolumnach na wąskiej stronie A4.
            int columns = portrait ? 2 : contentWidth >= WIDE_PAGE_CONTENT_WIDTH ? 4 : 3;
            float cellWidth = (contentWidth - CELL_GAP_X * (columns - 1)) / columns;

            List<Field> fields = content.fields() != null ? content.fields() : List.of();
            // Wysokość wiersza idzie za najdłuższą wartością. Sztywne trzy linie zostawiały puste pasy
            // między wierszami, bo większość odpowiedzi mieści się w jednej.
            int valueLines = 1;
            for (Field field : fields) {
                valueLines = Math.max(valueLines, valueLines(field, cellWidth).size());
            }
            float cellHeight = TEXT_SIZE + LABEL_GAP + valueLines * LINE_HEIGHT;
            for (int i = 0; i < fields.size(); i++) {
                float x = MARGIN + (i % columns) * (cellWidth + CELL_GAP_X);
                float top = page.contentTop() - (i / columns) * (cellHeight + CELL_GAP_Y);
                drawField(page.cs(), fields.get(i), x, top, cellWidth);
            }

            int rows = (fields.size() + columns - 1) / columns;
            float fieldsBottom = page.contentTop() - rows * cellHeight - Math.max(0, rows - 1) * CELL_GAP_Y;
            drawImages(page.cs(), rows == 0 ? page.contentTop() : fieldsBottom - 28f, contentWidth);
        }

        private void drawField(PDPageContentStream cs, Field field, float x, float top, float width) throws IOException {
            String label = sanitizeLine(field.label()).toUpperCase(POLISH);
            drawText(cs, fitLine(label, TEXT_SIZE, width), x, top - TEXT_SIZE, TEXT_SIZE, muted);

            List<String> lines = valueLines(field, width);
            float baseline = top - TEXT_SIZE - LABEL_GAP - TEXT_SIZE;
            for (String line : lines) {
                drawText(cs, line, x, baseline, TEXT_SIZE, text);
                baseline -= LINE_HEIGHT;
            }
        }

        private List<String> valueLines(Field field, float width) throws IOException {
            String value = sanitizeLine(field.value() != null ? field.value().replaceAll("\\R", " ") : "").trim();
            return wrap(value.isEmpty() ? "-" : value, TEXT_SIZE, width, VALUE_MAX_LINES);
        }

        private void drawImages(PDPageContentStream cs, float top, float contentWidth) throws IOException {
            List<PDImageXObject> images = new ArrayList<>();
            for (byte[] data : content.images() != null ? content.images() : List.<byte[]>of()) {
                if (images.size() == MAX_IMAGES) {
                    break;
                }
                PDImageXObject image = toImage(data, MAX_PHOTO_SIDE_PX);
                if (image != null) {
                    images.add(image);
                }
            }

            if (images.isEmpty()) {
                return;
            }

            // Zdjęcia idą rzędami o wspólnej wysokości i stałym odstępie. W równych polach, w których zdjęcie leży
            // na środku, widoczny odstęp zależałby od proporcji każdego zdjęcia. Poziomo wszystkie są w jednym
            // rzędzie, w pionie w dwóch.
            int perRow = portrait ? (images.size() + 1) / 2 : images.size();
            List<List<PDImageXObject>> rows = new ArrayList<>();
            for (int from = 0; from < images.size(); from += perRow) {
                rows.add(images.subList(from, Math.min(from + perRow, images.size())));
            }

            // Wysokość, przy której najciaśniejszy rząd wypełnia całą szerokość treści, nie wyższa niż limit.
            float rowHeight = IMAGE_ROW_MAX_HEIGHT;
            for (List<PDImageXObject> row : rows) {
                float aspects = 0;
                for (PDImageXObject image : row) {
                    aspects += (float) image.getWidth() / image.getHeight();
                }
                rowHeight = Math.min(rowHeight, (contentWidth - IMAGE_GAP * (row.size() - 1)) / aspects);
            }

            // Pasek zdjęć idzie zaraz pod danymi, a nadmiar wysokości zostaje pustym marginesem na dole strony.
            float areaTop = top - TEXT_SIZE - 12f;
            float available = areaTop - MARGIN;
            if (available < 60f) {
                return;
            }
            float gaps = IMAGE_GAP * (rows.size() - 1);
            if (rows.size() * rowHeight + gaps > available) {
                rowHeight = (available - gaps) / rows.size();
            }
            drawText(cs, "NADESŁANE INSPIRACJE", MARGIN, top - TEXT_SIZE, TEXT_SIZE, muted);

            float y = areaTop;
            for (List<PDImageXObject> row : rows) {
                float x = MARGIN;
                for (PDImageXObject image : row) {
                    float width = rowHeight * image.getWidth() / image.getHeight();
                    cs.drawImage(image, x, y - rowHeight, width, rowHeight);
                    x += width + IMAGE_GAP;
                }
                y -= rowHeight + IMAGE_GAP;
            }
        }

        // ---- page 2: Wycena ----

        void drawPricingPages() throws IOException {
            float contentWidth = contentWidth();
            String description = content.decorationDescription();
            boolean hasDescription = description != null && !description.isBlank();
            // Without a description there's nothing to share the row with, so the table takes the full width.
            float tableWidth = portrait || !hasDescription ? contentWidth : contentWidth * LANDSCAPE_TABLE_SHARE;

            Cursor table = new Cursor(0, pricingPage(0).contentTop());
            drawTable(table, MARGIN, tableWidth);

            if (!hasDescription) {
                return;
            }
            if (portrait) {
                // Portrait: the description continues below the table.
                drawDescription(new Cursor(table.page, table.y - 28f), MARGIN, contentWidth, description);
            } else {
                // Landscape: the description gets its own column to the right of the table.
                float descX = MARGIN + tableWidth + COLUMN_GAP;
                drawDescription(new Cursor(0, pricingPage(0).contentTop()), descX, contentWidth - tableWidth - COLUMN_GAP, description);
            }
        }

        private void drawTable(Cursor cursor, float x, float width) throws IOException {
            float sumRight = x + width;
            float unitRight = sumRight - COL_SUM - COL_GAP;
            float qtyRight = unitRight - COL_UNIT_PRICE - COL_GAP;
            float nameWidth = qtyRight - COL_QTY - COL_GAP - x;

            drawTableHeader(cursor, x, width, qtyRight, unitRight, sumRight);

            List<EventElement> elements = content.elements() != null ? content.elements() : List.of();
            BigDecimal total = BigDecimal.ZERO;

            if (elements.isEmpty()) {
                float rowHeight = TABLE_ROW_PADDING * 2 + LINE_HEIGHT;
                drawText(pricingPage(cursor.page).cs(), "Brak wycenionych elementów",
                        x, cursor.y - TABLE_ROW_PADDING - TEXT_SIZE, TEXT_SIZE, muted);
                cursor.y -= rowHeight;
            }

            for (EventElement element : elements) {
                String name = sanitizeLine(element.getName()).trim();
                List<String> nameLines = wrap(name.isEmpty() ? "-" : name, TEXT_SIZE, nameWidth, Integer.MAX_VALUE);
                float rowHeight = TABLE_ROW_PADDING * 2 + nameLines.size() * LINE_HEIGHT;
                if (cursor.needsNewPage(rowHeight)) {
                    cursor.nextPage();
                    drawTableHeader(cursor, x, width, qtyRight, unitRight, sumRight);
                }

                PDPageContentStream cs = pricingPage(cursor.page).cs();
                BigDecimal sum = element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity()));
                float baseline = cursor.y - TABLE_ROW_PADDING - TEXT_SIZE;
                for (int i = 0; i < nameLines.size(); i++) {
                    drawText(cs, nameLines.get(i), x, baseline - i * LINE_HEIGHT, TEXT_SIZE, text);
                }
                drawTextRight(cs, String.valueOf(element.getQuantity()), qtyRight, baseline, TEXT_SIZE, text);
                drawTextRight(cs, formatMoney(element.getUnitPrice()), unitRight, baseline, TEXT_SIZE, text);
                drawTextRight(cs, formatMoney(sum), sumRight, baseline, TEXT_SIZE, text);

                cursor.y -= rowHeight;
                drawLine(cs, x, cursor.y, sumRight, cursor.y, divider, 0.5f);
                total = total.add(sum);
            }

            if (cursor.needsNewPage(TABLE_TOTAL_HEIGHT)) {
                cursor.nextPage();
            }
            PDPageContentStream cs = pricingPage(cursor.page).cs();
            float baseline = cursor.y - 24f;
            drawTextRight(cs, "Razem", unitRight, baseline, TEXT_SIZE, muted);
            drawTextRight(cs, formatMoney(total), sumRight, baseline, TEXT_SIZE, text);
            cursor.y -= TABLE_TOTAL_HEIGHT;
        }

        private void drawTableHeader(Cursor cursor, float x, float width,
                                     float qtyRight, float unitRight, float sumRight) throws IOException {
            PDPageContentStream cs = pricingPage(cursor.page).cs();
            float baseline = cursor.y - TEXT_SIZE;
            drawText(cs, "NAZWA", x, baseline, TEXT_SIZE, muted);
            drawTextRight(cs, "ILOŚĆ", qtyRight, baseline, TEXT_SIZE, muted);
            drawTextRight(cs, "CENA ZA SZT.", unitRight, baseline, TEXT_SIZE, muted);
            drawTextRight(cs, "SUMA", sumRight, baseline, TEXT_SIZE, muted);
            cursor.y -= TABLE_HEADER_HEIGHT;
            drawLine(cs, x, cursor.y, x + width, cursor.y, divider, 0.75f);
        }

        private void drawDescription(Cursor cursor, float x, float width, String description) throws IOException {
            if (cursor.needsNewPage(TEXT_SIZE + 12f + LINE_HEIGHT)) {
                cursor.nextPage();
            }
            drawText(pricingPage(cursor.page).cs(), "OPIS DEKORACJI", x, cursor.y - TEXT_SIZE, TEXT_SIZE, muted);
            cursor.y -= TEXT_SIZE + 12f;

            for (String paragraph : description.split("\\R", -1)) {
                for (String line : wrap(sanitizeLine(paragraph), TEXT_SIZE, width, Integer.MAX_VALUE)) {
                    if (cursor.needsNewPage(LINE_HEIGHT)) {
                        cursor.nextPage();
                    }
                    drawText(pricingPage(cursor.page).cs(), line, x, cursor.y - TEXT_SIZE, TEXT_SIZE, text);
                    cursor.y -= LINE_HEIGHT;
                }
            }
        }

        /** Pricing pages are created on demand — "Wycena", then "Wycena (cd.)" when content overflows. */
        private PageArea pricingPage(int index) throws IOException {
            while (pricingPages.size() <= index) {
                pricingPages.add(newPage(pricingPages.isEmpty() ? PRICING_TITLE : PRICING_CONTINUED_TITLE));
            }
            return pricingPages.get(index);
        }

        private final class Cursor {
            int page;
            float y;

            Cursor(int page, float y) {
                this.page = page;
                this.y = y;
            }

            boolean needsNewPage(float height) {
                return y - height < MARGIN;
            }

            void nextPage() throws IOException {
                page++;
                y = pricingPage(page).contentTop();
            }
        }

        // ---- page chrome ----

        private PageArea newPage(String title) throws IOException {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);
            streams.add(cs);

            // From here on the stream draws in layout units; the scale maps them onto the real page.
            cs.transform(Matrix.getScaleInstance(scale, scale));

            float width = layoutSize.getWidth();
            cs.setNonStrokingColor(background);
            cs.addRect(0, 0, width, layoutSize.getHeight());
            cs.fill();

            float headerTop = layoutSize.getHeight() - MARGIN;
            float headerBottom = headerTop - HEADER_HEIGHT;
            float centerY = headerTop - HEADER_HEIGHT / 2;

            float logoSpace = 0;
            if (logo != null) {
                float scale = Math.min(LOGO_MAX_WIDTH / logo.getWidth(), HEADER_HEIGHT / logo.getHeight());
                float logoWidth = logo.getWidth() * scale;
                float logoHeight = logo.getHeight() * scale;
                cs.drawImage(logo, width - MARGIN - logoWidth, centerY - logoHeight / 2, logoWidth, logoHeight);
                logoSpace = logoWidth + 24f;
            }

            // Baseline offset by half the font's cap height, so the title sits centred on the logo whatever the font.
            float titleBaseline = centerY - TITLE_SIZE * capHeight(titleFont) / 2;
            // Tytuły są wersalikami; toUpperCase(POLISH), żeby „ó”, „ł” i „ę” dostały właściwe wielkie litery.
            drawText(cs, titleFont, fitLine(titleFont, title.toUpperCase(POLISH), TITLE_SIZE, width - 2 * MARGIN - logoSpace),
                    MARGIN, titleBaseline, TITLE_SIZE, text);
            drawLine(cs, MARGIN, headerBottom, width - MARGIN, headerBottom, divider, 0.75f);

            return new PageArea(cs, headerBottom - HEADER_GAP);
        }

        void closeStreams() throws IOException {
            IOException failure = null;
            for (PDPageContentStream cs : streams) {
                try {
                    cs.close();
                } catch (IOException e) {
                    failure = e;
                }
            }
            if (failure != null) {
                throw failure;
            }
        }

        private float contentWidth() {
            return layoutSize.getWidth() - 2 * MARGIN;
        }

        // ---- drawing primitives ----

        private void drawText(PDPageContentStream cs, String value, float x, float y, float size, Color color) throws IOException {
            drawText(cs, font, value, x, y, size, color);
        }

        private void drawText(PDPageContentStream cs, PDFont textFont, String value, float x, float y, float size, Color color) throws IOException {
            if (value.isEmpty()) {
                return;
            }
            cs.beginText();
            cs.setFont(textFont, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, y);
            cs.showText(value);
            cs.endText();
        }

        private void drawTextRight(PDPageContentStream cs, String value, float right, float y, float size, Color color) throws IOException {
            drawText(cs, value, right - textWidth(value, size), y, size, color);
        }

        private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color color, float lineWidth) throws IOException {
            cs.setStrokingColor(color);
            cs.setLineWidth(lineWidth);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private float textWidth(String value, float size) throws IOException {
            return textWidth(font, value, size);
        }

        private float textWidth(PDFont textFont, String value, float size) throws IOException {
            return textFont.getStringWidth(value) / 1000f * size;
        }

        // ---- text handling ----

        /** Client-entered text goes through here: tabs become spaces, control chars and glyphs the font lacks (e.g. emoji) are dropped. */
        private String sanitizeLine(String value) {
            if (value == null) {
                return "";
            }
            StringBuilder result = new StringBuilder(value.length());
            value.codePoints().forEach(codePoint -> {
                if (codePoint == '\t' || Character.isSpaceChar(codePoint)) {
                    result.append(' ');
                } else if (!Character.isISOControl(codePoint) && canEncode(codePoint)) {
                    result.appendCodePoint(codePoint);
                }
            });
            return result.toString();
        }

        private boolean canEncode(int codePoint) {
            return encodable.computeIfAbsent(codePoint, cp -> {
                try {
                    font.encode(new String(Character.toChars(cp)));
                    return true;
                } catch (IllegalArgumentException | IOException e) {
                    return false;
                }
            });
        }

        /** Word-wraps to maxWidth, breaking words longer than a line; beyond maxLines the last line ends with an ellipsis. */
        private List<String> wrap(String value, float size, float maxWidth, int maxLines) throws IOException {
            return wrap(font, value, size, maxWidth, maxLines);
        }

        private List<String> wrap(PDFont textFont, String value, float size, float maxWidth, int maxLines) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : value.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                while (textWidth(textFont, word, size) > maxWidth) {
                    int cut = fittingPrefixLength(textFont, word, size, maxWidth);
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    lines.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textWidth(textFont, candidate, size) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (!line.isEmpty() || lines.isEmpty()) {
                lines.add(line.toString());
            }

            if (lines.size() <= maxLines) {
                return lines;
            }
            List<String> truncated = new ArrayList<>(lines.subList(0, maxLines));
            truncated.set(maxLines - 1, ellipsize(textFont, truncated.get(maxLines - 1), size, maxWidth));
            return truncated;
        }

        private String fitLine(String value, float size, float maxWidth) throws IOException {
            return fitLine(font, value, size, maxWidth);
        }

        private String fitLine(PDFont textFont, String value, float size, float maxWidth) throws IOException {
            return wrap(textFont, value, size, maxWidth, 1).get(0);
        }

        private int fittingPrefixLength(PDFont textFont, String word, float size, float maxWidth) throws IOException {
            int end = word.offsetByCodePoints(0, 1);
            while (end < word.length()) {
                int next = word.offsetByCodePoints(end, 1);
                if (textWidth(textFont, word.substring(0, next), size) > maxWidth) {
                    break;
                }
                end = next;
            }
            return end;
        }

        private String ellipsize(PDFont textFont, String line, float size, float maxWidth) throws IOException {
            String ellipsis = canEncode('…') ? "…" : "...";
            String result = line.stripTrailing();
            while (!result.isEmpty() && textWidth(textFont, result + ellipsis, size) > maxWidth) {
                result = result.substring(0, result.offsetByCodePoints(result.length(), -1)).stripTrailing();
            }
            return result + ellipsis;
        }

        // ---- images ----

        /** Decodes and downsizes an image; photos become JPEG to keep the PDF small, images with transparency stay lossless. */
        private PDImageXObject toImage(byte[] data, int maxSidePx) throws IOException {
            BufferedImage source;
            try {
                source = ImageIO.read(new ByteArrayInputStream(data));
            } catch (IOException e) {
                return null;
            }
            if (source == null) {
                return null;
            }
            boolean alpha = source.getColorModel().hasAlpha();
            BufferedImage image = normalize(source, maxSidePx, alpha);
            return alpha
                    ? LosslessFactory.createFromImage(document, image)
                    : JPEGFactory.createFromImage(document, image, 0.85f);
        }
    }

    private record PageArea(PDPageContentStream cs, float contentTop) {
    }

    /** A missing font file must not take PDF generation down — warn and use the bundled Inter instead. */
    private static PDFont loadFont(PDDocument document, String path) throws IOException {
        try (InputStream fontStream = OfferPdfRenderer.class.getResourceAsStream(path)) {
            if (fontStream != null) {
                return PDType0Font.load(document, fontStream, true);
            }
        }
        log.warn("Brak czcionki {} na classpath — używam {}.", path, FALLBACK_FONT_PATH);
        try (InputStream fallback = OfferPdfRenderer.class.getResourceAsStream(FALLBACK_FONT_PATH)) {
            if (fallback == null) {
                throw new IllegalStateException("Font not found on classpath: " + FALLBACK_FONT_PATH);
            }
            return PDType0Font.load(document, fallback, true);
        }
    }

    /** Cap height in em, read from the font itself; 0.7 when the file doesn't say. */
    private static float capHeight(PDFont font) {
        PDFontDescriptor descriptor = font.getFontDescriptor();
        float height = descriptor != null ? descriptor.getCapHeight() / 1000f : 0f;
        return height > 0 ? height : 0.7f;
    }

    private static float onReferencePage(float points) {
        return points / REFERENCE_SCALE;
    }

    /** Redraws into a plain RGB/ARGB image — JPEGFactory can't take indexed images — scaled down to maxSidePx. */
    private static BufferedImage normalize(BufferedImage source, int maxSidePx, boolean alpha) {
        double scale = Math.min(1.0, (double) maxSidePx / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static Color parseColor(String hex) {
        try {
            return hex != null ? Color.decode(hex) : Color.WHITE;
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    /** Dark or light text, whichever contrasts more with the background (WCAG contrast ratio). */
    private static Color readableTextColor(Color background) {
        Color dark = new Color(34, 36, 48);
        Color light = new Color(246, 246, 248);
        double backgroundLuminance = luminance(background);
        return contrast(luminance(dark), backgroundLuminance) >= contrast(luminance(light), backgroundLuminance) ? dark : light;
    }

    private static double contrast(double a, double b) {
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    /** Blends a towards b by t (0 = a, 1 = b). */
    private static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(POLISH);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value != null ? value : BigDecimal.ZERO) + " zł";
    }
}
