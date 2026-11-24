package com.bank.corebanking_prod.feature.auth.security;

import com.bank.corebanking_prod.common.exception.LoginRateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptRateLimiterTest {

    @Test
    @DisplayName("Client is throttled after configured failures and released after a successful login")
    void shouldThrottleAndResetClient() {
        LoginAttemptRateLimiter limiter = new LoginAttemptRateLimiter(
                2, Duration.ofMinutes(15), 100,
                Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC)
        );
        limiter.recordFailure("127.0.0.1");
        limiter.recordFailure("127.0.0.1");

        assertThrows(LoginRateLimitExceededException.class, () -> limiter.checkAllowed("127.0.0.1"));
        assertDoesNotThrow(() -> limiter.checkAllowed("127.0.0.2"));

        limiter.recordSuccess("127.0.0.1");
        assertDoesNotThrow(() -> limiter.checkAllowed("127.0.0.1"));
    }

    @Test
    @DisplayName("Expired failure windows no longer block a client")
    void shouldExpireFailureWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-24T08:00:00Z"));
        LoginAttemptRateLimiter limiter = new LoginAttemptRateLimiter(1, Duration.ofMinutes(1), 100, clock);
        limiter.recordFailure("127.0.0.1");
        assertThrows(LoginRateLimitExceededException.class, () -> limiter.checkAllowed("127.0.0.1"));

        clock.advance(Duration.ofMinutes(2));
        assertDoesNotThrow(() -> limiter.checkAllowed("127.0.0.1"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
