package com.bank.corebanking_prod.feature.auth.event;

import java.time.Instant;

/**
 * Credential-free security event. SPEC-015 can persist these events without
 * receiving usernames, passwords, hashes, tokens, or authentication headers.
 */
public record AuthenticationAuditEvent(
        Instant occurredAt,
        Outcome outcome,
        Long actorIdentityId,
        Long targetIdentityId,
        String action
) {

    public enum Outcome {
        SUCCESS,
        FAILURE
    }
}
