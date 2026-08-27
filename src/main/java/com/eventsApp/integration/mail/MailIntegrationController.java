package com.eventsApp.integration.mail;

import com.eventsApp.integration.mail.model.dto.MailConnectionStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/integrations")
public class MailIntegrationController {

    private final MailIntegrationService mailIntegrationService;
    private final GoogleOAuthProperties properties;

    /**
     * Returns the Google consent URL instead of redirecting, so the browser call still carries the JWT.
     * The frontend navigates to the returned URL.
     */
    @GetMapping("/google/connect")
    public ResponseEntity<Map<String, String>> connect() {
        return ResponseEntity.ok(Map.of("authorizationUrl", mailIntegrationService.buildAuthorizationUrl()));
    }

    /** Google redirects the browser here — no JWT is present, the tenant comes from the one-time state. */
    @GetMapping("/google/callback")
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            return redirectToFrontend("error", "Autoryzacja Google została anulowana.");
        }
        if (code == null || state == null) {
            return redirectToFrontend("error", "Niekompletna odpowiedź autoryzacyjna Google.");
        }
        try {
            mailIntegrationService.completeConnection(code, state);
            return redirectToFrontend("connected", null);
        } catch (RuntimeException e) {
            return redirectToFrontend("error", e.getMessage());
        }
    }

    @GetMapping("/mail/status")
    public ResponseEntity<MailConnectionStatusDTO> status() {
        return ResponseEntity.ok(mailIntegrationService.getStatus());
    }

    @DeleteMapping("/mail")
    public ResponseEntity<Void> disconnect() {
        mailIntegrationService.disconnect();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private RedirectView redirectToFrontend(String result, String message) {
        StringBuilder url = new StringBuilder(properties.getFrontendRedirectUri())
                .append("?mail=").append(result);
        if (message != null) {
            url.append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
        }
        return new RedirectView(url.toString());
    }
}
