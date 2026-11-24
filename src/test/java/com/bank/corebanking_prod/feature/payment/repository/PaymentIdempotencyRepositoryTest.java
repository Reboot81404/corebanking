package com.bank.corebanking_prod.feature.payment.repository;

import com.bank.corebanking_prod.feature.payment.model.entity.PaymentIdempotencyEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PaymentIdempotencyRepositoryTest {

    @Autowired
    private PaymentIdempotencyRepository repository;

    @Test
    @DisplayName("Kayıt başarıyla kaydedilmeli ve operation + idempotencyKey ile okunabilmeli")
    void shouldSaveAndFindIdempotencyRecord() {
        PaymentIdempotencyEntity entity = new PaymentIdempotencyEntity(
                0L,
                "PAYMENT_TRANSFER_V1",
                "test-key-1234567890",
                "hash123",
                200,
                "{\"message\":\"Transfer is completed.\"}",
                1L
        );

        PaymentIdempotencyEntity saved = repository.saveAndFlush(entity);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<PaymentIdempotencyEntity> found = repository.findByPrincipalIdAndOperationAndIdempotencyKey(
                0L,
                "PAYMENT_TRANSFER_V1",
                "test-key-1234567890"
        );

        assertTrue(found.isPresent());
        assertEquals("hash123", found.get().getRequestHash());
        assertEquals(200, found.get().getResponseStatus());
    }

    @Test
    @DisplayName("Aynı operation ve idempotencyKey ile ikinci kayıt eklendiğinde unique constraint ihlali fırlatılmalı")
    void shouldThrowExceptionWhenDuplicateOperationAndKey() {
        PaymentIdempotencyEntity first = new PaymentIdempotencyEntity(
                "PAYMENT_TRANSFER_V1",
                "duplicate-key-12345",
                "hash1",
                200,
                "payload1",
                1L
        );
        repository.saveAndFlush(first);

        PaymentIdempotencyEntity second = new PaymentIdempotencyEntity(
                "PAYMENT_TRANSFER_V1",
                "duplicate-key-12345",
                "hash2",
                200,
                "payload2",
                2L
        );

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(second));
    }
    @Test
    @DisplayName("Aynı key farklı authenticated principal kapsamlarında bağımsız kaydedilebilmeli")
    void shouldAllowSameKeyForDifferentPrincipals() {
        PaymentIdempotencyEntity first = new PaymentIdempotencyEntity(
                11L, "PAYMENT_TRANSFER_V1", "shared-key-123456", "hash1", 200, "payload1", 11L);
        PaymentIdempotencyEntity second = new PaymentIdempotencyEntity(
                12L, "PAYMENT_TRANSFER_V1", "shared-key-123456", "hash2", 200, "payload2", 12L);

        repository.saveAndFlush(first);
        PaymentIdempotencyEntity savedSecond = repository.saveAndFlush(second);

        assertNotNull(savedSecond.getId());
        assertTrue(repository.findByPrincipalIdAndOperationAndIdempotencyKey(
                11L, "PAYMENT_TRANSFER_V1", "shared-key-123456").isPresent());
        assertTrue(repository.findByPrincipalIdAndOperationAndIdempotencyKey(
                12L, "PAYMENT_TRANSFER_V1", "shared-key-123456").isPresent());
    }

}
