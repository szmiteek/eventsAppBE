package com.eventsApp.integration.mail;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.integration.mail.model.MailConnectionStatus;
import com.eventsApp.integration.mail.model.MailProvider;
import com.eventsApp.integration.mail.model.TenantMailConnection;
import com.eventsApp.integration.mail.model.dto.MailConnectionStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MailIntegrationService {

    private final CurrentTenantProvider currentTenantProvider;
    private final TenantMailConnectionRepository connectionRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final GoogleOAuthClient googleOAuthClient;
    private final OAuthStateStore stateStore;
    private final GoogleOAuthProperties properties;

    /** Builds the Google consent URL for the tenant taken from the security context. */
    public String buildAuthorizationUrl() {
        if (!properties.isConfigured()) {
            throw new EventApiException(
                    "Integracja z Google nie jest skonfigurowana po stronie serwera.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        int tenantId = currentTenantProvider.requireTenantId();
        return googleOAuthClient.buildAuthorizationUrl(stateStore.issue(tenantId));
    }

    /**
     * Completes the OAuth handshake. The tenant comes from the single-use state token, never from the request,
     * so a callback cannot be replayed against another tenant.
     */
    public void completeConnection(String code, String state) {
        int tenantId = stateStore.consume(state)
                .orElseThrow(() -> new EventApiException(
                        "Nieprawidłowy lub wygasły link autoryzacyjny. Spróbuj połączyć konto ponownie.",
                        HttpStatus.BAD_REQUEST));

        GoogleOAuthClient.TokenResponse tokens = googleOAuthClient.exchangeCode(code);
        if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
            throw new EventApiException(
                    "Google nie zwróciło refresh tokena. Odłącz aplikację w ustawieniach konta Google i spróbuj ponownie.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!tokens.hasGmailSendScope()) {
            throw new EventApiException(
                    "Nie przyznano zgody na wysyłanie e-maili. Połącz konto ponownie i zaznacz uprawnienie "
                            + "\"Wysyłanie wiadomości e-mail w Twoim imieniu\".",
                    HttpStatus.BAD_REQUEST);
        }

        String accountEmail = googleOAuthClient.fetchAccountEmail(tokens.accessToken());

        TenantMailConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantMailConnection.builder()
                        .tenantId(tenantId)
                        .createdAt(LocalDateTime.now())
                        .build());

        connection.setProvider(MailProvider.GOOGLE);
        connection.setEmail(accountEmail);
        connection.setEncryptedRefreshToken(tokenEncryptionService.encrypt(tokens.refreshToken()));
        connection.setStatus(MailConnectionStatus.ACTIVE);
        connection.setUpdatedAt(LocalDateTime.now());
        connectionRepository.save(connection);
    }

    public MailConnectionStatusDTO getStatus() {
        int tenantId = currentTenantProvider.requireTenantId();
        return connectionRepository.findByTenantId(tenantId)
                .map(connection -> MailConnectionStatusDTO.builder()
                        .connected(connection.getStatus() == MailConnectionStatus.ACTIVE)
                        .provider(connection.getProvider())
                        .email(connection.getEmail())
                        .status(connection.getStatus())
                        .connectedAt(connection.getCreatedAt())
                        .build())
                .orElseGet(() -> MailConnectionStatusDTO.builder()
                        .connected(false)
                        .status(MailConnectionStatus.DISCONNECTED)
                        .build());
    }

    public void disconnect() {
        int tenantId = currentTenantProvider.requireTenantId();
        connectionRepository.findByTenantId(tenantId).ifPresent(connectionRepository::delete);
    }
}
