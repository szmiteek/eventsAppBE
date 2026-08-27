package com.eventsApp.integration.mail;

import com.eventsApp.exceptions.EventApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    public static final String GMAIL_SEND_SCOPE = "https://www.googleapis.com/auth/gmail.send";

    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final GoogleOAuthProperties properties;
    private final RestClient restClient = RestClient.create();

    public String buildAuthorizationUrl(String state) {
        return AUTH_ENDPOINT
                + "?client_id=" + encode(properties.getClientId())
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(GMAIL_SEND_SCOPE + " https://www.googleapis.com/auth/userinfo.email")
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + encode(state);
    }

    /** Exchanges the one-time authorization code for tokens. */
    public TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        Map<?, ?> body = postForm(form, "Nie udało się wymienić kodu autoryzacyjnego Google.");
        return new TokenResponse(
                stringValue(body, "access_token"),
                stringValue(body, "refresh_token"),
                stringValue(body, "scope"));
    }

    /** Trades a stored refresh token for a short-lived access token. */
    public String refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "refresh_token");

        Map<?, ?> body = postForm(form, "Autoryzacja Google wygasła.");
        String accessToken = stringValue(body, "access_token");
        if (accessToken == null) {
            throw new GoogleReauthRequiredException("Google nie zwróciło tokena dostępu.");
        }
        return accessToken;
    }

    public String fetchAccountEmail(String accessToken) {
        try {
            Map<?, ?> body = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            return body != null ? stringValue(body, "email") : null;
        } catch (Exception e) {
            throw new EventApiException("Nie udało się pobrać adresu e-mail z konta Google.", HttpStatus.BAD_GATEWAY);
        }
    }

    private Map<?, ?> postForm(MultiValueMap<String, String> form, String errorMessage) {
        try {
            return restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new GoogleReauthRequiredException(errorMessage);
        }
    }

    private String stringValue(Map<?, ?> body, String key) {
        Object value = body.get(key);
        return value != null ? value.toString() : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    public record TokenResponse(String accessToken, String refreshToken, String grantedScope) {

        /** Google drops scopes the user declined on the consent screen, so the grant must be checked. */
        public boolean hasGmailSendScope() {
            return grantedScope != null && grantedScope.contains(GMAIL_SEND_SCOPE);
        }
    }
}
