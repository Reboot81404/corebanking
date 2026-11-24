package com.bank.corebanking_prod.feature.ledger.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccountEntity ledgerAccount;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    public JournalEntryLineEntity() {}

    public JournalEntryLineEntity(
            LedgerAccountEntity ledgerAccount,
            BigDecimal debitAmount,
            BigDecimal creditAmount) {
        this.ledgerAccount = ledgerAccount;
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JournalEntryEntity getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntryEntity journalEntry) {
        this.journalEntry = journalEntry;
    }

    public LedgerAccountEntity getLedgerAccount() {
        return ledgerAccount;
    }

    public void setLedgerAccount(LedgerAccountEntity ledgerAccount) {
        this.ledgerAccount = ledgerAccount;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }
}
