package com.bank.corebanking_prod.feature.ledger.domain;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryLineEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;

import java.math.BigDecimal;
import java.util.List;

public final class LedgerDomainValidator {

    private LedgerDomainValidator() {}

    /**
     * Verifies that the given journal entry satisfies double-entry bookkeeping invariants:
     * 1. Entry currency is not null.
     * 2. Lines list contains at least 2 lines.
     * 3. Every line references an account with currency matching the entry currency.
     * 4. Every line has non-negative amounts, and exactly one side (debit or credit) is strictly positive.
     * 5. Total debit amount equals total credit amount.
     */
    public static void validateEntryInvariants(JournalEntryEntity entry) {
        if (entry == null) {
            throw new BusinessException("Journal entry cannot be null.");
        }
        if (entry.getCurrency() == null) {
            throw new BusinessException("Journal entry currency cannot be null.");
        }
        List<JournalEntryLineEntity> lines = entry.getLines();
        if (lines == null || lines.size() < 2) {
            throw new BusinessException("A journal entry must contain at least two lines.");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntryLineEntity line : lines) {
            if (line.getLedgerAccount() == null) {
                throw new BusinessException("Journal entry line must reference a ledger account.");
            }
            if (line.getLedgerAccount().getCurrency() != entry.getCurrency()) {
                throw new BusinessException("Ledger account currency must match journal entry currency.");
            }

            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;

            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Debit and credit amounts cannot be negative.");
            }

            boolean debitPositive = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean creditPositive = credit.compareTo(BigDecimal.ZERO) > 0;

            if ((debitPositive && creditPositive) || (!debitPositive && !creditPositive)) {
                throw new BusinessException("A journal line must have exactly one positive side (debit or credit).");
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(String.format(
                    "Journal entry is unbalanced. Total Debit: %s, Total Credit: %s",
                    totalDebit, totalCredit
            ));
        }

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total entry amount must be strictly positive.");
        }
    }

    /**
     * Calculates balance for a ledger account based on its normal balance:
     * - CUSTOMER_DEPOSIT (liability, CREDIT): totalCredit - totalDebit
     * - OPENING_BALANCE_CLEARING (asset/clearing, DEBIT): totalDebit - totalCredit
     */
    public static BigDecimal calculateAccountBalance(
            LedgerAccountType accountType,
            NormalBalance normalBalance,
            BigDecimal totalDebit,
            BigDecimal totalCredit) {

        BigDecimal debit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        BigDecimal credit = totalCredit != null ? totalCredit : BigDecimal.ZERO;

        if (normalBalance == NormalBalance.CREDIT || accountType == LedgerAccountType.CUSTOMER_DEPOSIT) {
            return credit.subtract(debit);
        } else {
            return debit.subtract(credit);
        }
    }
}
