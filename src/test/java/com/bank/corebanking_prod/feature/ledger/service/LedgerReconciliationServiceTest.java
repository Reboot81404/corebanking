package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.service.AccountService;
import com.bank.corebanking_prod.feature.ledger.model.dto.ReconciliationReportDto;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryLineRepository;
import com.bank.corebanking_prod.feature.ledger.repository.LedgerAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerReconciliationServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    @InjectMocks
    private LedgerReconciliationServiceImpl reconciliationService;

    @Test
    @DisplayName("Tüm hesaplar mutabık olduğunda discrepancyCount = 0 dönmeli")
    void shouldReportZeroDiscrepancyWhenFullyReconciled() {
        AccountDto.Response acc = new AccountDto.Response(
                1L, "TR111", new BigDecimal("500.00"), Currency.TRY, 10L, LocalDateTime.now()
        );
        when(accountService.getAllAccounts()).thenReturn(List.of(acc));

        LedgerAccountEntity la = new LedgerAccountEntity(
                "DEP-TR111", "Deposit", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY, 1L
        );
        la.setId(10L);
        when(ledgerAccountRepository.findByBankAccountId(1L)).thenReturn(Optional.of(la));

        when(journalEntryLineRepository.sumDebitByLedgerAccountId(10L)).thenReturn(new BigDecimal("100.00"));
        when(journalEntryLineRepository.sumCreditByLedgerAccountId(10L)).thenReturn(new BigDecimal("600.00"));

        ReconciliationReportDto report = reconciliationService.reconcileCustomerDeposits();

        assertNotNull(report);
        assertEquals(1, report.getTotalAccountsChecked());
        assertEquals(1, report.getMatchedAccounts());
        assertEquals(0, report.getDiscrepancyCount());
        assertTrue(report.isFullyReconciled());
    }

    @Test
    @DisplayName("Bakiye tutarsızlığı olduğunda farkı raporlamalı ve otomatik düzeltme yapmamalı")
    void shouldReportDiscrepancyWithoutAlteringBalances() {
        AccountDto.Response acc = new AccountDto.Response(
                1L, "TR111", new BigDecimal("500.00"), Currency.TRY, 10L, LocalDateTime.now()
        );
        when(accountService.getAllAccounts()).thenReturn(List.of(acc));

        LedgerAccountEntity la = new LedgerAccountEntity(
                "DEP-TR111", "Deposit", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY, 1L
        );
        la.setId(10L);
        when(ledgerAccountRepository.findByBankAccountId(1L)).thenReturn(Optional.of(la));

        // Ledger balance = 600 - 200 = 400.00 (Account has 500.00)
        when(journalEntryLineRepository.sumDebitByLedgerAccountId(10L)).thenReturn(new BigDecimal("200.00"));
        when(journalEntryLineRepository.sumCreditByLedgerAccountId(10L)).thenReturn(new BigDecimal("600.00"));

        ReconciliationReportDto report = reconciliationService.reconcileCustomerDeposits();

        assertNotNull(report);
        assertEquals(1, report.getTotalAccountsChecked());
        assertEquals(0, report.getMatchedAccounts());
        assertEquals(1, report.getDiscrepancyCount());
        assertFalse(report.isFullyReconciled());

        ReconciliationReportDto.DiscrepancyItem item = report.getDiscrepancies().get(0);
        assertEquals(1L, item.getBankAccountId());
        assertEquals(new BigDecimal("500.00"), item.getAccountBalance());
        assertEquals(new BigDecimal("400.00"), item.getLedgerBalance());
        assertEquals(new BigDecimal("100.00"), item.getDifference());
    }

    @Test
    @DisplayName("Eksik ledger hesabı olan banka hesaplarını missing listesinde raporlamalı")
    void shouldReportMissingLedgerAccounts() {
        AccountDto.Response acc = new AccountDto.Response(
                2L, "TR222", BigDecimal.ZERO, Currency.TRY, 10L, LocalDateTime.now()
        );
        when(accountService.getAllAccounts()).thenReturn(List.of(acc));
        when(ledgerAccountRepository.findByBankAccountId(2L)).thenReturn(Optional.empty());

        ReconciliationReportDto report = reconciliationService.reconcileCustomerDeposits();

        assertEquals(1, report.getMissingLedgerAccountBankIds().size());
        assertEquals(2L, report.getMissingLedgerAccountBankIds().get(0));
        assertFalse(report.isFullyReconciled());
    }
}
