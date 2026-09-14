package com.eventsApp.pdf;

import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.offerSettings.model.PdfOrientation;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
public class OfferPdfRenderer {

    public static final int MAX_IMAGES = 4;

    private static final String FONT_PATH = "/fonts/Inter.ttf";
    private static final String INFO_TITLE = "Informacje ogólne";
    private static final String PRICING_TITLE = "Wycena";
    private static final String PRICING_CONTINUED_TITLE = "Wycena (cd.)";
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    private static final float MARGIN = 40f;
    private static final float HEADER_HEIGHT = 56f;
    private static final float HEADER_GAP = 24f;
    private static final float TITLE_SIZE = 24f;
    private static final float LOGO_MAX_WIDTH = 150f;

    private static final float LABEL_SIZE = 8.5f;
    private static final float LABEL_GAP = 8f;
    private static final float VALUE_SIZE = 12f;
    private static final float VALUE_LINE_HEIGHT = 16f;
    private static final int VALUE_MAX_LINES = 3;
    private static final float CELL_GAP_X = 24f;
    private static final float CELL_GAP_Y = 20f;
    private static final float IMAGE_GAP = 16f;
    private static final int MAX_PHOTO_SIDE_PX = 1600;
    private static final int MAX_LOGO_SIDE_PX = 1000;

    private static final float TABLE_TEXT_SIZE = 10.5f;
    private static final float TABLE_LINE_HEIGHT = 14f;
    private static final float TABLE_ROW_PADDING = 8f;
    private static final float TABLE_HEADER_HEIGHT = 20f;
    private static final float TABLE_TOTAL_HEIGHT = 34f;
    private static final float COL_QTY = 50f;
    private static final float COL_UNIT_PRICE = 85f;
    private static final float COL_SUM = 90f;
    private static final float COL_GAP = 10f;
    private static final float COLUMN_GAP = 32f;
    private static final float LANDSCAPE_TABLE_SHARE = 0.58f;
    private static final float DESC_TEXT_SIZE = 11f;
    private static final float DESC_LINE_HEIGHT = 16f;

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

    public byte[] render(Content content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            Layout layout = new Layout(document, content);
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
        private final PDFont font;
        private final PDRectangle pageSize;
        private final boolean portrait;
        private final Color background;
        private final Color text;
        private final Color muted;
        private final Color divider;
        private final PDImageXObject logo;
        private final List<PDPageContentStream> streams = new ArrayList<>();
        private final List<PageArea> pricingPages = new ArrayList<>();
        private final Map<Integer, Boolean> encodable = new HashMap<>();

        Layout(PDDocument document, Content content) throws IOException {
            this.document = document;
            this.content = content;
            this.font = loadFont(document);
            this.portrait = content.orientation() == PdfOrientation.PORTRAIT;
            this.pageSize = portrait
                    ? PDRectangle.A4
                    : new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
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
            int columns = portrait ? 2 : 4;
            float cellWidth = (contentWidth - CELL_GAP_X * (columns - 1)) / columns;
            float cellHeight = LABEL_SIZE + LABEL_GAP + VALUE_MAX_LINES * VALUE_LINE_HEIGHT;

            List<Field> fields = content.fields() != null ? content.fields() : List.of();
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
            drawText(cs, fitLine(label, LABEL_SIZE, width), x, top - LABEL_SIZE, LABEL_SIZE, muted);

            String value = sanitizeLine(field.value() != null ? field.value().replaceAll("\\R", " ") : "").trim();
            List<String> lines = wrap(value.isEmpty() ? "-" : value, VALUE_SIZE, width, VALUE_MAX_LINES);
            float baseline = top - LABEL_SIZE - LABEL_GAP - VALUE_SIZE;
            for (String line : lines) {
                drawText(cs, line, x, baseline, VALUE_SIZE, text);
                baseline -= VALUE_LINE_HEIGHT;
            }
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

            float areaTop = top - LABEL_SIZE - 12f;
            float areaHeight = areaTop - MARGIN;
            if (images.isEmpty() || areaHeight < 60f) {
                return;
            }
            drawText(cs, "NADESŁANE INSPIRACJE", MARGIN, top - LABEL_SIZE, LABEL_SIZE, muted);

            // Landscape: one row of four. Portrait: a 2×2 grid.
            int columns = portrait ? 2 : MAX_IMAGES;
            int rows = portrait ? 2 : 1;
            float boxWidth = (contentWidth - IMAGE_GAP * (columns - 1)) / columns;
            float boxHeight = (areaHeight - IMAGE_GAP * (rows - 1)) / rows;

            for (int i = 0; i < images.size(); i++) {
                PDImageXObject image = images.get(i);
                float boxX = MARGIN + (i % columns) * (boxWidth + IMAGE_GAP);
                float boxTop = areaTop - (i / columns) * (boxHeight + IMAGE_GAP);
                float scale = Math.min(boxWidth / image.getWidth(), boxHeight / image.getHeight());
                float drawWidth = image.getWidth() * scale;
                float drawHeight = image.getHeight() * scale;
                cs.drawImage(image, boxX + (boxWidth - drawWidth) / 2, boxTop - drawHeight, drawWidth, drawHeight);
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
                float rowHeight = TABLE_ROW_PADDING * 2 + TABLE_LINE_HEIGHT;
                drawText(pricingPage(cursor.page).cs(), "Brak wycenionych elementów",
                        x, cursor.y - TABLE_ROW_PADDING - TABLE_TEXT_SIZE, TABLE_TEXT_SIZE, muted);
                cursor.y -= rowHeight;
            }

            for (EventElement element : elements) {
                String name = sanitizeLine(element.getName()).trim();
                List<String> nameLines = wrap(name.isEmpty() ? "-" : name, TABLE_TEXT_SIZE, nameWidth, Integer.MAX_VALUE);
                float rowHeight = TABLE_ROW_PADDING * 2 + nameLines.size() * TABLE_LINE_HEIGHT;
                if (cursor.needsNewPage(rowHeight)) {
                    cursor.nextPage();
                    drawTableHeader(cursor, x, width, qtyRight, unitRight, sumRight);
                }

                PDPageContentStream cs = pricingPage(cursor.page).cs();
                BigDecimal sum = element.getUnitPrice().multiply(BigDecimal.valueOf(element.getQuantity()));
                float baseline = cursor.y - TABLE_ROW_PADDING - TABLE_TEXT_SIZE;
                for (int i = 0; i < nameLines.size(); i++) {
                    drawText(cs, nameLines.get(i), x, baseline - i * TABLE_LINE_HEIGHT, TABLE_TEXT_SIZE, text);
                }
                drawTextRight(cs, String.valueOf(element.getQuantity()), qtyRight, baseline, TABLE_TEXT_SIZE, text);
                drawTextRight(cs, formatMoney(element.getUnitPrice()), unitRight, baseline, TABLE_TEXT_SIZE, text);
                drawTextRight(cs, formatMoney(sum), sumRight, baseline, TABLE_TEXT_SIZE, text);

                cursor.y -= rowHeight;
                drawLine(cs, x, cursor.y, sumRight, cursor.y, divider, 0.5f);
                total = total.add(sum);
            }

            if (cursor.needsNewPage(TABLE_TOTAL_HEIGHT)) {
                cursor.nextPage();
            }
            PDPageContentStream cs = pricingPage(cursor.page).cs();
            float baseline = cursor.y - 22f;
            drawTextRight(cs, "Razem", unitRight, baseline, 11f, muted);
            drawTextRight(cs, formatMoney(total), sumRight, baseline, 12f, text);
            cursor.y -= TABLE_TOTAL_HEIGHT;
        }

        private void drawTableHeader(Cursor cursor, float x, float width,
                                     float qtyRight, float unitRight, float sumRight) throws IOException {
            PDPageContentStream cs = pricingPage(cursor.page).cs();
            float baseline = cursor.y - LABEL_SIZE;
            drawText(cs, "NAZWA", x, baseline, LABEL_SIZE, muted);
            drawTextRight(cs, "ILOŚĆ", qtyRight, baseline, LABEL_SIZE, muted);
            drawTextRight(cs, "CENA ZA SZT.", unitRight, baseline, LABEL_SIZE, muted);
            drawTextRight(cs, "SUMA", sumRight, baseline, LABEL_SIZE, muted);
            cursor.y -= TABLE_HEADER_HEIGHT;
            drawLine(cs, x, cursor.y, x + width, cursor.y, divider, 0.75f);
        }

        private void drawDescription(Cursor cursor, float x, float width, String description) throws IOException {
            if (cursor.needsNewPage(LABEL_SIZE + 12f + DESC_LINE_HEIGHT)) {
                cursor.nextPage();
            }
            drawText(pricingPage(cursor.page).cs(), "OPIS DEKORACJI", x, cursor.y - LABEL_SIZE, LABEL_SIZE, muted);
            cursor.y -= LABEL_SIZE + 12f;

            for (String paragraph : description.split("\\R", -1)) {
                for (String line : wrap(sanitizeLine(paragraph), DESC_TEXT_SIZE, width, Integer.MAX_VALUE)) {
                    if (cursor.needsNewPage(DESC_LINE_HEIGHT)) {
                        cursor.nextPage();
                    }
                    drawText(pricingPage(cursor.page).cs(), line, x, cursor.y - DESC_TEXT_SIZE, DESC_TEXT_SIZE, text);
                    cursor.y -= DESC_LINE_HEIGHT;
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

            float width = pageSize.getWidth();
            cs.setNonStrokingColor(background);
            cs.addRect(0, 0, width, pageSize.getHeight());
            cs.fill();

            float headerTop = pageSize.getHeight() - MARGIN;
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

            // Cap height of Inter is ~0.73 em — offset the baseline so the title sits centred on the logo.
            float titleBaseline = centerY - TITLE_SIZE * 0.365f;
            drawText(cs, fitLine(title, TITLE_SIZE, width - 2 * MARGIN - logoSpace), MARGIN, titleBaseline, TITLE_SIZE, text);
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
            return pageSize.getWidth() - 2 * MARGIN;
        }

        // ---- drawing primitives ----

        private void drawText(PDPageContentStream cs, String value, float x, float y, float size, Color color) throws IOException {
            if (value.isEmpty()) {
                return;
            }
            cs.beginText();
            cs.setFont(font, size);
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
            return font.getStringWidth(value) / 1000f * size;
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
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : value.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                while (textWidth(word, size) > maxWidth) {
                    int cut = fittingPrefixLength(word, size, maxWidth);
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
                if (textWidth(candidate, size) <= maxWidth) {
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
            truncated.set(maxLines - 1, ellipsize(truncated.get(maxLines - 1), size, maxWidth));
            return truncated;
        }

        private String fitLine(String value, float size, float maxWidth) throws IOException {
            return wrap(value, size, maxWidth, 1).get(0);
        }

        private int fittingPrefixLength(String word, float size, float maxWidth) throws IOException {
            int end = word.offsetByCodePoints(0, 1);
            while (end < word.length()) {
                int next = word.offsetByCodePoints(end, 1);
                if (textWidth(word.substring(0, next), size) > maxWidth) {
                    break;
                }
                end = next;
            }
            return end;
        }

        private String ellipsize(String line, float size, float maxWidth) throws IOException {
            String ellipsis = canEncode('…') ? "…" : "...";
            String result = line.stripTrailing();
            while (!result.isEmpty() && textWidth(result + ellipsis, size) > maxWidth) {
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

    private static PDFont loadFont(PDDocument document) throws IOException {
        try (InputStream fontStream = OfferPdfRenderer.class.getResourceAsStream(FONT_PATH)) {
            if (fontStream == null) {
                throw new IllegalStateException("Font not found on classpath: " + FONT_PATH);
            }
            return PDType0Font.load(document, fontStream, true);
        }
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
