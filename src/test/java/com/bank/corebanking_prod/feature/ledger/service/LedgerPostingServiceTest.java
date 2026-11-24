package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryStatus;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryType;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerPostingServiceTest {

    @Mock
    private LedgerAccountService ledgerAccountService;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private LedgerPostingServiceImpl ledgerPostingService;

    private LedgerAccountEntity senderLedgerAccount;
    private LedgerAccountEntity receiverLedgerAccount;

    @BeforeEach
    void setUp() {
        senderLedgerAccount = new LedgerAccountEntity(
                "DEP-TR1000",
                "Customer Deposit 1",
                LedgerAccountType.CUSTOMER_DEPOSIT,
                NormalBalance.CREDIT,
                Currency.TRY,
                1L
        );
        senderLedgerAccount.setId(10L);

        receiverLedgerAccount = new LedgerAccountEntity(
                "DEP-TR2000",
                "Customer Deposit 2",
                LedgerAccountType.CUSTOMER_DEPOSIT,
                NormalBalance.CREDIT,
                Currency.TRY,
                2L
        );
        receiverLedgerAccount.setId(20L);
    }

    @Test
    @DisplayName("Başarılı transferde dengeli journal entry ve 2 line oluşturulmalı")
    void shouldPostInternalTransferSuccessfully() {
        when(journalEntryRepository.existsByTransactionHistoryId(100L)).thenReturn(false);
        when(ledgerAccountService.getCustomerDepositAccount(1L)).thenReturn(senderLedgerAccount);
        when(ledgerAccountService.getCustomerDepositAccount(2L)).thenReturn(receiverLedgerAccount);
        when(journalEntryRepository.save(any(JournalEntryEntity.class))).thenAnswer(i -> i.getArgument(0));

        JournalEntryEntity result = ledgerPostingService.postInternalTransfer(
                100L,
                1L,
                2L,
                new BigDecimal("250.00"),
                Currency.TRY
        );

        assertNotNull(result);
        assertEquals(JournalEntryType.INTERNAL_TRANSFER, result.getTransactionType());
        assertEquals(JournalEntryStatus.POSTED, result.getStatus());
        assertEquals(Currency.TRY, result.getCurrency());
        assertEquals(2, result.getLines().size());

        // Line 1: sender debit
        assertEquals(new BigDecimal("250.00"), result.getLines().get(0).getDebitAmount());
        assertEquals(BigDecimal.ZERO, result.getLines().get(0).getCreditAmount());
        assertEquals(senderLedgerAccount, result.getLines().get(0).getLedgerAccount());

        // Line 2: receiver credit
        assertEquals(BigDecimal.ZERO, result.getLines().get(1).getDebitAmount());
        assertEquals(new BigDecimal("250.00"), result.getLines().get(1).getCreditAmount());
        assertEquals(receiverLedgerAccount, result.getLines().get(1).getLedgerAccount());

        verify(journalEntryRepository).save(any(JournalEntryEntity.class));
    }

    @Test
    @DisplayName("Aynı transaction history id için mükerrer journal oluşturulamaz")
    void shouldThrowWhenJournalAlreadyExistsForHistory() {
        when(journalEntryRepository.existsByTransactionHistoryId(100L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                ledgerPostingService.postInternalTransfer(
                        100L,
                        1L,
                        2L,
                        new BigDecimal("100.00"),
                        Currency.TRY
                )
        );

        assertTrue(ex.getMessage().contains("already exists"));
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer para birimi ile ledger hesabı para birimi uyuşmazsa transfer reddedilmeli")
    void shouldThrowWhenCurrencyMismatch() {
        receiverLedgerAccount.setCurrency(Currency.USD);
        when(journalEntryRepository.existsByTransactionHistoryId(100L)).thenReturn(false);
        when(ledgerAccountService.getCustomerDepositAccount(1L)).thenReturn(senderLedgerAccount);
        when(ledgerAccountService.getCustomerDepositAccount(2L)).thenReturn(receiverLedgerAccount);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                ledgerPostingService.postInternalTransfer(
                        100L,
                        1L,
                        2L,
                        new BigDecimal("100.00"),
                        Currency.TRY
                )
        );

        assertTrue(ex.getMessage().contains("currency does not match"));
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sıfır veya negatif transfer tutarı reddedilmeli")
    void shouldThrowWhenAmountIsZeroOrNegative() {
        assertThrows(BusinessException.class, () ->
                ledgerPostingService.postInternalTransfer(
                        100L,
                        1L,
                        2L,
                        BigDecimal.ZERO,
                        Currency.TRY
                )
        );
    }
}
