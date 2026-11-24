package com.bank.corebanking_prod.feature.account.service;

import com.bank.corebanking_prod.feature.account.repository.AccountNumberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbanGeneratorServiceTest {

    @Mock
    private AccountNumberRepository accountNumberRepository;

    private IbanGeneratorService ibanGeneratorService;

    @BeforeEach
    void setUp() {
        ibanGeneratorService = new IbanGeneratorService(accountNumberRepository, "00000");
    }

    @Test
    void shouldGenerateValidTurkishIban() {
        when(accountNumberRepository.getNextAccountNumber())
                .thenReturn(100_000_000_000_000L);

        String iban = ibanGeneratorService.generate();

        assertEquals(26, iban.length());
        assertTrue(iban.matches("TR[0-9]{24}"));
        assertEquals("TR71", iban.substring(0, 4));
        assertTrue(ibanGeneratorService.isValid(iban));
    }

    @Test
    void shouldGenerateUniqueIbansForDifferentSequenceValues() {
        AtomicLong sequence = new AtomicLong(100_000_000_000_000L);
        when(accountNumberRepository.getNextAccountNumber())
                .thenAnswer(invocation -> sequence.getAndIncrement());

        Set<String> generatedIbans = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            generatedIbans.add(ibanGeneratorService.generate());
        }

        assertEquals(1_000, generatedIbans.size());
    }

    @Test
    void shouldRejectInvalidChecksum() {
        assertFalse(ibanGeneratorService.isValid("TR000000000000000000000000"));
    }

    @Test
    void shouldRejectInvalidLength() {
        assertFalse(ibanGeneratorService.isValid("TR123"));
    }

    @Test
    void shouldRejectNullIban() {
        assertFalse(ibanGeneratorService.isValid(null));
    }

    @Test
    void shouldRejectBankCodeThatDoesNotContainFiveDigits() {
        IbanGeneratorService serviceWithInvalidBankCode =
                new IbanGeneratorService(accountNumberRepository, "123");

        assertThrows(IllegalArgumentException.class, serviceWithInvalidBankCode::generate);
    }

    @Test
    void shouldRejectSequenceValueThatExceedsSixteenDigits() {
        when(accountNumberRepository.getNextAccountNumber())
                .thenReturn(10_000_000_000_000_000L);

        assertThrows(IllegalStateException.class, ibanGeneratorService::generate);
    }
}
