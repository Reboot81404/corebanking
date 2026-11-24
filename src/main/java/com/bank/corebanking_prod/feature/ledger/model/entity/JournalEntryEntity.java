package com.bank.corebanking_prod.feature.ledger.model.entity;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryStatus;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 64)
    private String referenceNumber;

    @Column(name = "transaction_history_id", unique = true)
    private Long transactionHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private JournalEntryType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JournalEntryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLineEntity> lines = new ArrayList<>();

    public JournalEntryEntity() {}

    public JournalEntryEntity(
            String referenceNumber,
            Long transactionHistoryId,
            JournalEntryType transactionType,
            Currency currency,
            String description,
            JournalEntryStatus status) {

        this.referenceNumber = referenceNumber;
        this.transactionHistoryId = transactionHistoryId;
        this.transactionType = transactionType;
        this.currency = currency;
        this.description = description;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.postedAt == null) {
            this.postedAt = LocalDateTime.now();
        }
    }

    public void addLine(JournalEntryLineEntity line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Long getTransactionHistoryId() {
        return transactionHistoryId;
    }

    public void setTransactionHistoryId(Long transactionHistoryId) {
        this.transactionHistoryId = transactionHistoryId;
    }

    public JournalEntryType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(JournalEntryType transactionType) {
        this.transactionType = transactionType;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public void setStatus(JournalEntryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public List<JournalEntryLineEntity> getLines() {
        return lines;
    }

    public void setLines(List<JournalEntryLineEntity> lines) {
        this.lines = lines;
    }
}
