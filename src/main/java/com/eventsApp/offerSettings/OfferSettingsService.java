package com.eventsApp.offerSettings;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.PdfOrientation;
import com.eventsApp.offerSettings.model.TenantOfferCoverPdf;
import com.eventsApp.offerSettings.model.TenantOfferSettings;
import com.eventsApp.offerSettings.model.command.OfferSettingsUpdateCommand;
import com.eventsApp.offerSettings.model.dto.OfferSettingsDTO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.eventsApp.offerSettings.OfferSettingsMapper.mapToDTO;
import static com.eventsApp.offerSettings.model.OfferInfoField.COLORS;
import static com.eventsApp.offerSettings.model.OfferInfoField.EVENT_DATE;
import static com.eventsApp.offerSettings.model.OfferInfoField.FLOWERS_TYPE;
import static com.eventsApp.offerSettings.model.OfferInfoField.GUESTS;
import static com.eventsApp.offerSettings.model.OfferInfoField.GUESTS_TABLE_TYPE;
import static com.eventsApp.offerSettings.model.OfferInfoField.MAIN_TABLE_TYPE;
import static com.eventsApp.offerSettings.model.OfferInfoField.VENUE;

@Service
@RequiredArgsConstructor
public class OfferSettingsService {

    public static final int MAX_INFO_FIELDS = 8;

    private static final String DEFAULT_BACKGROUND_COLOR = "#FFFFFF";
    private static final PdfOrientation DEFAULT_ORIENTATION = PdfOrientation.LANDSCAPE;
    /** What the old fixed template printed, so offers keep looking familiar until the tenant customises them. */
    private static final List<OfferInfoField> DEFAULT_INFO_FIELDS =
            List.of(VENUE, EVENT_DATE, GUESTS, MAIN_TABLE_TYPE, GUESTS_TABLE_TYPE, FLOWERS_TYPE, COLORS);

    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private static final Set<String> LOGO_CONTENT_TYPES = Set.of("image/png", "image/jpeg");
    /** Matches spring.servlet.multipart.max-file-size — a bigger file is rejected by the server before it gets here. */
    private static final long MAX_COVER_PDF_BYTES = 10 * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final TenantOfferSettingsRepository tenantOfferSettingsRepository;
    private final TenantOfferCoverPdfRepository tenantOfferCoverPdfRepository;
    private final CurrentTenantProvider currentTenantProvider;

    /** The tenant's own PDF, with the name it was uploaded under. */
    public record CoverPdf(String filename, byte[] data) {
    }

    public OfferSettingsDTO getOwn() {
        return mapToDTO(resolveForTenant(currentTenantProvider.requireTenantId()));
    }

    public OfferSettingsDTO update(OfferSettingsUpdateCommand command) {
        TenantOfferSettings settings = resolveForTenant(currentTenantProvider.requireTenantId());
        settings.setBackgroundColor(command.getBackgroundColor().toUpperCase(Locale.ROOT));
        settings.setOrientation(command.getOrientation());
        // Keep the tenant's arrangement — it's the order the fields appear in the PDF. Duplicates are dropped.
        settings.setInfoFields(new ArrayList<>(command.getInfoFields().stream().distinct().toList()));
        return mapToDTO(tenantOfferSettingsRepository.save(settings));
    }

    public OfferSettingsDTO uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EventApiException("Nie wybrano pliku z logo.", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new EventApiException("Logo musi być plikiem PNG lub JPG.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new EventApiException("Logo może mieć maksymalnie 2 MB.", HttpStatus.BAD_REQUEST);
        }
        byte[] data = readBytes(file);
        if (!isReadableImage(data)) {
            throw new EventApiException("Nie udało się odczytać obrazu z logo.", HttpStatus.BAD_REQUEST);
        }

        TenantOfferSettings settings = resolveForTenant(currentTenantProvider.requireTenantId());
        settings.setLogoData(data);
        settings.setLogoContentType(contentType);
        settings.setLogoFilename(file.getOriginalFilename());
        return mapToDTO(tenantOfferSettingsRepository.save(settings));
    }

    public TenantOfferSettings getOwnLogo() {
        TenantOfferSettings settings = resolveForTenant(currentTenantProvider.requireTenantId());
        if (settings.getLogoData() == null || settings.getLogoData().length == 0) {
            throw new EventApiException("Logo not found", HttpStatus.NOT_FOUND);
        }
        return settings;
    }

    public OfferSettingsDTO deleteLogo() {
        TenantOfferSettings settings = resolveForTenant(currentTenantProvider.requireTenantId());
        settings.setLogoData(null);
        settings.setLogoContentType(null);
        settings.setLogoFilename(null);
        return mapToDTO(tenantOfferSettingsRepository.save(settings));
    }

    /** The tenant's own PDF (e.g. welcome pages); the generated offer pages are appended after it. */
    public OfferSettingsDTO uploadCoverPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EventApiException("Nie wybrano pliku PDF.", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!PDF_CONTENT_TYPE.equals(contentType)) {
            throw new EventApiException("Plik musi być w formacie PDF.", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_COVER_PDF_BYTES) {
            throw new EventApiException("Plik PDF może mieć maksymalnie 10 MB.", HttpStatus.BAD_REQUEST);
        }
        byte[] data = readBytes(file);
        int pages = countPdfPages(data);

        int tenantId = currentTenantProvider.requireTenantId();
        tenantOfferCoverPdfRepository.save(new TenantOfferCoverPdf(tenantId, data));

        TenantOfferSettings settings = resolveForTenant(tenantId);
        settings.setCoverPdfFilename(file.getOriginalFilename());
        settings.setCoverPdfPages(pages);
        return mapToDTO(tenantOfferSettingsRepository.save(settings));
    }

    public CoverPdf getOwnCoverPdf() {
        int tenantId = currentTenantProvider.requireTenantId();
        byte[] data = resolveCoverPdfData(tenantId)
                .orElseThrow(() -> new EventApiException("Cover PDF not found", HttpStatus.NOT_FOUND));
        String filename = resolveForTenant(tenantId).getCoverPdfFilename();
        return new CoverPdf(filename != null ? filename : "oferta-wstep.pdf", data);
    }

    public OfferSettingsDTO deleteCoverPdf() {
        int tenantId = currentTenantProvider.requireTenantId();
        tenantOfferCoverPdfRepository.findById(tenantId).ifPresent(tenantOfferCoverPdfRepository::delete);

        TenantOfferSettings settings = resolveForTenant(tenantId);
        settings.setCoverPdfFilename(null);
        settings.setCoverPdfPages(null);
        return mapToDTO(tenantOfferSettingsRepository.save(settings));
    }

    /** Loaded only when an offer PDF is generated or previewed — never together with the settings. */
    public Optional<byte[]> resolveCoverPdfData(int tenantId) {
        return tenantOfferCoverPdfRepository.findById(tenantId)
                .map(TenantOfferCoverPdf::getData)
                .filter(data -> data.length > 0);
    }

    /** The tenant's saved settings, or unsaved defaults when they never opened the settings page. */
    public TenantOfferSettings resolveForTenant(int tenantId) {
        return tenantOfferSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantOfferSettings.builder()
                        .tenantId(tenantId)
                        .backgroundColor(DEFAULT_BACKGROUND_COLOR)
                        .orientation(DEFAULT_ORIENTATION)
                        .infoFields(new ArrayList<>(DEFAULT_INFO_FIELDS.stream().sorted().toList()))
                        .build());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    private boolean isReadableImage(byte[] data) {
        try {
            return ImageIO.read(new ByteArrayInputStream(data)) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /** Also the file's validity check: a PDF that can't be opened here can't be merged with the offer either. */
    private int countPdfPages(byte[] data) {
        try (PDDocument document = Loader.loadPDF(data)) {
            if (document.isEncrypted()) {
                throw new EventApiException("Zabezpieczonego pliku PDF nie da się połączyć z ofertą.", HttpStatus.BAD_REQUEST);
            }
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new EventApiException("Nie udało się odczytać pliku PDF.", HttpStatus.BAD_REQUEST);
        }
    }
}
