package com.bank.corebanking_prod.feature.ledger.domain;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryLineEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryStatus;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryType;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LedgerDomainInvariantTest {

    private LedgerAccountEntity createAccount(String code, LedgerAccountType type, NormalBalance normalBalance, Currency currency) {
        return new LedgerAccountEntity(code, code, type, normalBalance, currency, 1L);
    }

    @Test
    @DisplayName("Dengeli ve geçerli iki satırlı entry invariant kontrolünden başarıyla geçmeli")
    void shouldPassWhenEntryIsBalancedAndValid() {
        LedgerAccountEntity senderAccount = createAccount("DEP-SENDER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);
        LedgerAccountEntity receiverAccount = createAccount("DEP-RECEIVER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);

        JournalEntryEntity entry = new JournalEntryEntity(
                "REF-001",
                100L,
                JournalEntryType.INTERNAL_TRANSFER,
                Currency.TRY,
                "Transfer 500 TRY",
                JournalEntryStatus.POSTED
        );

        entry.addLine(new JournalEntryLineEntity(senderAccount, new BigDecimal("500.00"), BigDecimal.ZERO));
        entry.addLine(new JournalEntryLineEntity(receiverAccount, BigDecimal.ZERO, new BigDecimal("500.00")));

        assertDoesNotThrow(() -> LedgerDomainValidator.validateEntryInvariants(entry));
    }

    @Test
    @DisplayName("Dengesiz entry (Debit != Credit) BusinessException fırlatmalı")
    void shouldThrowWhenEntryIsUnbalanced() {
        LedgerAccountEntity senderAccount = createAccount("DEP-SENDER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);
        LedgerAccountEntity receiverAccount = createAccount("DEP-RECEIVER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);

        JournalEntryEntity entry = new JournalEntryEntity(
                "REF-002",
                101L,
                JournalEntryType.INTERNAL_TRANSFER,
                Currency.TRY,
                "Transfer 500 TRY unbalanced",
                JournalEntryStatus.POSTED
        );

        entry.addLine(new JournalEntryLineEntity(senderAccount, new BigDecimal("500.00"), BigDecimal.ZERO));
        entry.addLine(new JournalEntryLineEntity(receiverAccount, BigDecimal.ZERO, new BigDecimal("499.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> LedgerDomainValidator.validateEntryInvariants(entry));
        assertTrue(ex.getMessage().contains("unbalanced"));
    }

    @Test
    @DisplayName("Tek satırlı entry kural ihlali olarak reddedilmeli (minimum 2 lines)")
    void shouldThrowWhenEntryHasLessThanTwoLines() {
        LedgerAccountEntity senderAccount = createAccount("DEP-SENDER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);

        JournalEntryEntity entry = new JournalEntryEntity(
                "REF-003",
                102L,
                JournalEntryType.INTERNAL_TRANSFER,
                Currency.TRY,
                "Transfer 500 TRY single line",
                JournalEntryStatus.POSTED
        );

        entry.addLine(new JournalEntryLineEntity(senderAccount, new BigDecimal("500.00"), BigDecimal.ZERO));

        BusinessException ex = assertThrows(BusinessException.class, () -> LedgerDomainValidator.validateEntryInvariants(entry));
        assertTrue(ex.getMessage().contains("at least two lines"));
    }

    @Test
    @DisplayName("Para birimi uyuşmazlığında entry reddedilmeli")
    void shouldThrowWhenLineCurrencyDoesNotMatchEntryCurrency() {
        LedgerAccountEntity senderAccount = createAccount("DEP-SENDER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);
        LedgerAccountEntity receiverAccount = createAccount("DEP-RECEIVER-USD", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.USD);

        JournalEntryEntity entry = new JournalEntryEntity(
                "REF-004",
                103L,
                JournalEntryType.INTERNAL_TRANSFER,
                Currency.TRY,
                "Mixed currency transfer",
                JournalEntryStatus.POSTED
        );

        entry.addLine(new JournalEntryLineEntity(senderAccount, new BigDecimal("100.00"), BigDecimal.ZERO));
        entry.addLine(new JournalEntryLineEntity(receiverAccount, BigDecimal.ZERO, new BigDecimal("100.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> LedgerDomainValidator.validateEntryInvariants(entry));
        assertTrue(ex.getMessage().contains("currency must match"));
    }

    @Test
    @DisplayName("Aynı satırda hem debit hem credit pozitif olamaz")
    void shouldThrowWhenBothDebitAndCreditPositiveOnSameLine() {
        LedgerAccountEntity senderAccount = createAccount("DEP-SENDER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);
        LedgerAccountEntity receiverAccount = createAccount("DEP-RECEIVER", LedgerAccountType.CUSTOMER_DEPOSIT, NormalBalance.CREDIT, Currency.TRY);

        JournalEntryEntity entry = new JournalEntryEntity(
                "REF-005",
                104L,
                JournalEntryType.INTERNAL_TRANSFER,
                Currency.TRY,
                "Invalid line amounts",
                JournalEntryStatus.POSTED
        );

        entry.addLine(new JournalEntryLineEntity(senderAccount, new BigDecimal("100.00"), new BigDecimal("50.00")));
        entry.addLine(new JournalEntryLineEntity(receiverAccount, BigDecimal.ZERO, new BigDecimal("150.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> LedgerDomainValidator.validateEntryInvariants(entry));
        assertTrue(ex.getMessage().contains("exactly one positive side"));
    }

    @Test
    @DisplayName("Müşteri mevduat hesap bakiyesi total_credit - total_debit olarak hesaplanmalı")
    void shouldCalculateCustomerDepositBalanceAsCreditMinusDebit() {
        BigDecimal balance = LedgerDomainValidator.calculateAccountBalance(
                LedgerAccountType.CUSTOMER_DEPOSIT,
                NormalBalance.CREDIT,
                new BigDecimal("200.00"), // Debit
                new BigDecimal("1000.00") // Credit
        );

        assertEquals(new BigDecimal("800.00"), balance);
    }

    @Test
    @DisplayName("Clearing hesabı normal bakiyesi total_debit - total_credit olarak hesaplanmalı")
    void shouldCalculateClearingAccountBalanceAsDebitMinusCredit() {
        BigDecimal balance = LedgerDomainValidator.calculateAccountBalance(
                LedgerAccountType.OPENING_BALANCE_CLEARING,
                NormalBalance.DEBIT,
                new BigDecimal("1000.00"), // Debit
                new BigDecimal("200.00")  // Credit
        );

        assertEquals(new BigDecimal("800.00"), balance);
    }
}
