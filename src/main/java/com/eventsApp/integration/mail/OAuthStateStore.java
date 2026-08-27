package com.eventsApp.integration.mail;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-use OAuth state tokens with a TTL. The state is the only thing that carries the tenant across
 * the Google redirect, so it is generated server-side from the authenticated context and consumed once.
 */
@Component
public class OAuthStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Map<String, StateEntry> states = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(int tenantId) {
        purgeExpired();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        states.put(state, new StateEntry(tenantId, Instant.now().plus(TTL)));
        return state;
    }

    public Optional<Integer> consume(String state) {
        purgeExpired();
        if (state == null) {
            return Optional.empty();
        }
        StateEntry entry = states.remove(state);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.tenantId());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record StateEntry(int tenantId, Instant expiresAt) {
    }
}
