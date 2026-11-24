package com.bank.corebanking_prod.feature.auth.event;

import java.time.Instant;

/**
 * Credential-free authorization decision event. Resource references are stable
 * internal IDs; raw TCKN, IBAN, request payloads and bearer tokens are excluded.
 */
public record AuthorizationAuditEvent(
        Instant occurredAt,
        Long actorIdentityId,
        Decision decision,
        String action,
        String resourceType,
        Long resourceId
) {

    public enum Decision {
        ALLOWED,
        DENIED
    }
}
