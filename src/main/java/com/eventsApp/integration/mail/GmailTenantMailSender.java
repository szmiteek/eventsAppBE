package com.eventsApp.integration.mail;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.exceptions.EventApiException;
import com.eventsApp.integration.mail.model.MailConnectionStatus;
import com.eventsApp.integration.mail.model.TenantMailConnection;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GmailTenantMailSender implements TenantMailSender {

    private static final String GMAIL_SEND_ENDPOINT =
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final CurrentTenantProvider currentTenantProvider;
    private final TenantMailConnectionRepository connectionRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final GoogleOAuthClient googleOAuthClient;
    private final RestClient restClient = RestClient.create();

    @Override
    public void send(MailMessage message) {
        int tenantId = currentTenantProvider.requireTenantId();

        TenantMailConnection connection = connectionRepository.findByTenantId(tenantId)
                .filter(c -> c.getStatus() == MailConnectionStatus.ACTIVE)
                .orElseThrow(() -> new EventApiException(
                        "Podłącz konto Google w Ustawieniach, zanim wyślesz ofertę.", HttpStatus.BAD_REQUEST));

        String accessToken;
        try {
            accessToken = googleOAuthClient.refreshAccessToken(
                    tokenEncryptionService.decrypt(connection.getEncryptedRefreshToken()));
        } catch (GoogleReauthRequiredException e) {
            markReauthRequired(connection);
            throw new EventApiException(
                    "Połączenie z kontem Google wygasło. Połącz konto ponownie w Ustawieniach.", HttpStatus.CONFLICT);
        }

        String rawMessage = buildRawMessage(connection.getEmail(), message);
        try {
            restClient.post()
                    .uri(GMAIL_SEND_ENDPOINT)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", rawMessage))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new EventApiException("Nie udało się wysłać e-maila przez Gmail: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private void markReauthRequired(TenantMailConnection connection) {
        connection.setStatus(MailConnectionStatus.REAUTH_REQUIRED);
        connection.setUpdatedAt(LocalDateTime.now());
        connectionRepository.save(connection);
    }

    /** Gmail's API takes a base64url-encoded RFC 2822 message. */
    private String buildRawMessage(String from, MailMessage message) {
        try {
            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body() != null ? message.body() : "", false);
            if (message.attachment() != null) {
                helper.addAttachment(message.attachmentFilename(), new ByteArrayResource(message.attachment()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mimeMessage.writeTo(out);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new EventApiException("Nie udało się przygotować wiadomości e-mail.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
