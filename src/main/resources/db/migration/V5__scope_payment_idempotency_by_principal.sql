-- Once authentication is enabled, an idempotency key belongs to the authenticated identity.
-- Existing rows have no historical actor information and remain in reserved principal_id 0.
ALTER TABLE payment_idempotency_records
    ADD COLUMN principal_id BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payment_idempotency_records
    DROP CONSTRAINT uk_payment_idempotency;

ALTER TABLE payment_idempotency_records
    ADD CONSTRAINT uk_payment_idempotency
        UNIQUE (principal_id, operation, idempotency_key);

-- The new composite UNIQUE constraint already provides the lookup index.
DROP INDEX IF EXISTS idx_payment_idempotency_lookup;

ALTER TABLE payment_idempotency_records
    ADD CONSTRAINT chk_payment_idempotency_principal_id
        CHECK (principal_id >= 0);
