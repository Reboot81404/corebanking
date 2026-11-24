package com.bank.corebanking_prod.feature.payment.util;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PayloadHashCalculatorTest {

    private final PayloadHashCalculator calculator = new PayloadHashCalculator();

    @Test
    @DisplayName("Aynı transfer değerleri (10, 10.0, 10.00) aynı hash çıktısını üretmeli")
    void shouldProduceSameHashForDifferentScales() {
        TransactionHistoryDto.TransferRequest req1 = new TransactionHistoryDto.TransferRequest(
                "TR100000000000000000000001",
                "TR200000000000000000000002",
                new BigDecimal("10")
        );

        TransactionHistoryDto.TransferRequest req2 = new TransactionHistoryDto.TransferRequest(
                "TR100000000000000000000001",
                "TR200000000000000000000002",
                new BigDecimal("10.0")
        );

        TransactionHistoryDto.TransferRequest req3 = new TransactionHistoryDto.TransferRequest(
                "TR100000000000000000000001",
                "TR200000000000000000000002",
                new BigDecimal("10.00")
        );

        String hash1 = calculator.calculateHash(req1);
        String hash2 = calculator.calculateHash(req2);
        String hash3 = calculator.calculateHash(req3);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
        assertEquals(64, hash1.length());
    }

    @Test
    @DisplayName("Payload alanlarından biri değişirse farklı hash üretilmeli")
    void shouldProduceDifferentHashWhenPayloadChanges() {
        TransactionHistoryDto.TransferRequest req1 = new TransactionHistoryDto.TransferRequest(
                "TR100000000000000000000001",
                "TR200000000000000000000002",
                new BigDecimal("100.00")
        );

        TransactionHistoryDto.TransferRequest req2 = new TransactionHistoryDto.TransferRequest(
                "TR100000000000000000000001",
                "TR200000000000000000000002",
                new BigDecimal("100.01")
        );

        assertNotEquals(calculator.calculateHash(req1), calculator.calculateHash(req2));
    }

    @Test
    @DisplayName("Geçerli ve geçersiz Idempotency-Key doğrulaması")
    void shouldValidateKeyFormat() {
        // Geçerli anahtarlar
        assertDoesNotThrow(() -> calculator.validateKey("1234567890123456"));
        assertDoesNotThrow(() -> calculator.validateKey("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"));
        assertDoesNotThrow(() -> calculator.validateKey("VALID_KEY_WITH_UNDERSCORE_12345"));

        // Geçersiz anahtarlar
        assertThrows(BusinessException.class, () -> calculator.validateKey(null));
        assertThrows(BusinessException.class, () -> calculator.validateKey("short-key"));
        assertThrows(BusinessException.class, () -> calculator.validateKey("key-with-special!@#$%^"));
        assertThrows(BusinessException.class, () -> calculator.validateKey("a".repeat(65))); // >64
    }
}
