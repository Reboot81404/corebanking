package com.bank.corebanking_prod.feature.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import com.bank.corebanking_prod.common.exception.LoginRateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;



/** Process-local fallback limiter. A gateway limiter is still required for clustered deployments. */
@Component
public class LoginAttemptRateLimiter {

    private final int maximumAttempts;
    private final Duration window;
    private final int maximumTrackedClients;
    private final Clock clock;
    private final Map<String, AttemptWindow> attempts = new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public LoginAttemptRateLimiter(
            @Value("${security.login.max-attempts:10}") int maximumAttempts,
            @Value("${security.login.window-seconds:900}") long windowSeconds,
            @Value("${security.login.max-tracked-clients:10000}") int maximumTrackedClients) {
        this(maximumAttempts, Duration.ofSeconds(windowSeconds), maximumTrackedClients, Clock.systemUTC());
    }

    LoginAttemptRateLimiter(int maximumAttempts, Duration window, int maximumTrackedClients, Clock clock) {
        if (maximumAttempts < 1 || window.isZero() || window.isNegative() || maximumTrackedClients < 1) {
            throw new IllegalArgumentException("Login rate-limit settings must be positive.");
        }
        this.maximumAttempts = maximumAttempts;
        this.window = window;
        this.maximumTrackedClients = maximumTrackedClients;
        this.clock = clock;
    }

    public synchronized void checkAllowed(String clientAddress) {
        String key = normalizeClientAddress(clientAddress);
        AttemptWindow current = getCurrentWindow(key);
        if (current != null && current.failures >= maximumAttempts) {
            throw new LoginRateLimitExceededException("Too many login attempts. Try again later.");
        }
    }

    public synchronized void recordFailure(String clientAddress) {
        String key = normalizeClientAddress(clientAddress);
        Instant now = clock.instant();
        AttemptWindow current = getCurrentWindow(key);
        if (current == null) {
            current = new AttemptWindow(now, 0);
            attempts.put(key, current);
        }
        current.failures++;
        evictExpiredAndOverflow(now);
    }

    public synchronized void recordSuccess(String clientAddress) {
        attempts.remove(normalizeClientAddress(clientAddress));
    }

    private AttemptWindow getCurrentWindow(String key) {
        AttemptWindow current = attempts.get(key);
        if (current != null && current.startedAt.plus(window).isBefore(clock.instant())) {
            attempts.remove(key);
            return null;
        }
        return current;
    }

    private void evictExpiredAndOverflow(Instant now) {
        Iterator<Map.Entry<String, AttemptWindow>> iterator = attempts.entrySet().iterator();
        while (iterator.hasNext()) {
            AttemptWindow windowEntry = iterator.next().getValue();
            if (windowEntry.startedAt.plus(window).isBefore(now)) {
                iterator.remove();
            }
        }
        while (attempts.size() > maximumTrackedClients) {
            attempts.remove(attempts.keySet().iterator().next());
        }
    }

    private String normalizeClientAddress(String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            return "unknown-client";
        }
        return clientAddress.trim();
    }

    private static final class AttemptWindow {
        private final Instant startedAt;
        private int failures;

        private AttemptWindow(Instant startedAt, int failures) {
            this.startedAt = startedAt;
            this.failures = failures;
        }
    }
}
