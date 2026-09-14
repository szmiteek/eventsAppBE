package com.eventsApp.offerSettings;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.offerSettings.model.OfferInfoField;
import com.eventsApp.offerSettings.model.PdfOrientation;
import com.eventsApp.offerSettings.model.TenantOfferSettings;
import com.eventsApp.offerSettings.model.command.OfferSettingsUpdateCommand;
import com.eventsApp.offerSettings.model.dto.OfferSettingsDTO;
import lombok.RequiredArgsConstructor;
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

    private final TenantOfferSettingsRepository tenantOfferSettingsRepository;
    private final CurrentTenantProvider currentTenantProvider;

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
            throw new UncheckedIOException("Failed to read uploaded logo", e);
        }
    }

    private boolean isReadableImage(byte[] data) {
        try {
            return ImageIO.read(new ByteArrayInputStream(data)) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
