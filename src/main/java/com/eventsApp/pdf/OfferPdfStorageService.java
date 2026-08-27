package com.eventsApp.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class OfferPdfStorageService {

    private final Path baseDir;

    public OfferPdfStorageService(@Value("${app.storage.offer-pdf-dir}") String offerPdfDir) {
        this.baseDir = Path.of(offerPdfDir);
    }

    public void save(int tenantId, int offerId, byte[] pdf) {
        Path path = resolvePath(tenantId, offerId);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, pdf);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save offer PDF to disk: " + path, e);
        }
    }

    public Optional<byte[]> load(int tenantId, int offerId) {
        Path path = resolvePath(tenantId, offerId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read offer PDF from disk: " + path, e);
        }
    }

    private Path resolvePath(int tenantId, int offerId) {
        return baseDir.resolve(String.valueOf(tenantId)).resolve("oferta-" + offerId + ".pdf");
    }
}
