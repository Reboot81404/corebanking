package com.bank.corebanking_prod.feature.ledger.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationReportDto {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private int totalAccountsChecked;
    private int matchedAccounts;
    private int discrepancyCount;
    private List<DiscrepancyItem> discrepancies = new ArrayList<>();
    private List<Long> missingLedgerAccountBankIds = new ArrayList<>();

    public ReconciliationReportDto() {}

    public ReconciliationReportDto(
            int totalAccountsChecked,
            int matchedAccounts,
            int discrepancyCount,
            List<DiscrepancyItem> discrepancies,
            List<Long> missingLedgerAccountBankIds) {
        this.totalAccountsChecked = totalAccountsChecked;
        this.matchedAccounts = matchedAccounts;
        this.discrepancyCount = discrepancyCount;
        this.discrepancies = discrepancies;
        this.missingLedgerAccountBankIds = missingLedgerAccountBankIds;
    }

    public boolean isFullyReconciled() {
        return discrepancyCount == 0 && missingLedgerAccountBankIds.isEmpty();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getTotalAccountsChecked() {
        return totalAccountsChecked;
    }

    public void setTotalAccountsChecked(int totalAccountsChecked) {
        this.totalAccountsChecked = totalAccountsChecked;
    }

    public int getMatchedAccounts() {
        return matchedAccounts;
    }

    public void setMatchedAccounts(int matchedAccounts) {
        this.matchedAccounts = matchedAccounts;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(int discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public List<DiscrepancyItem> getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(List<DiscrepancyItem> discrepancies) {
        this.discrepancies = discrepancies;
    }

    public List<Long> getMissingLedgerAccountBankIds() {
        return missingLedgerAccountBankIds;
    }

    public void setMissingLedgerAccountBankIds(List<Long> missingLedgerAccountBankIds) {
        this.missingLedgerAccountBankIds = missingLedgerAccountBankIds;
    }

    public static class DiscrepancyItem {
        private Long bankAccountId;
        private String iban;
        private BigDecimal accountBalance;
        private BigDecimal ledgerBalance;
        private BigDecimal difference;

        public DiscrepancyItem() {}

        public DiscrepancyItem(
                Long bankAccountId,
                String iban,
                BigDecimal accountBalance,
                BigDecimal ledgerBalance,
                BigDecimal difference) {
            this.bankAccountId = bankAccountId;
            this.iban = iban;
            this.accountBalance = accountBalance;
            this.ledgerBalance = ledgerBalance;
            this.difference = difference;
        }

        public Long getBankAccountId() {
            return bankAccountId;
        }

        public void setBankAccountId(Long bankAccountId) {
            this.bankAccountId = bankAccountId;
        }

        public String getIban() {
            return iban;
        }

        public void setIban(String iban) {
            this.iban = iban;
        }

        public BigDecimal getAccountBalance() {
            return accountBalance;
        }

        public void setAccountBalance(BigDecimal accountBalance) {
            this.accountBalance = accountBalance;
        }

        public BigDecimal getLedgerBalance() {
            return ledgerBalance;
        }

        public void setLedgerBalance(BigDecimal ledgerBalance) {
            this.ledgerBalance = ledgerBalance;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }
    }
}
