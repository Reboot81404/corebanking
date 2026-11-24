package com.bank.corebanking_prod.feature.ledger.model.entity;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, unique = true, length = 64)
    private String accountCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 40)
    private LedgerAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 20)
    private NormalBalance normalBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "bank_account_id", unique = true)
    private Long bankAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LedgerAccountEntity() {}

    public LedgerAccountEntity(
            String accountCode,
            String name,
            LedgerAccountType accountType,
            NormalBalance normalBalance,
            Currency currency,
            Long bankAccountId) {

        this.accountCode = accountCode;
        this.name = name;
        this.accountType = accountType;
        this.normalBalance = normalBalance;
        this.currency = currency;
        this.bankAccountId = bankAccountId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(LedgerAccountType accountType) {
        this.accountType = accountType;
    }

    public NormalBalance getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(NormalBalance normalBalance) {
        this.normalBalance = normalBalance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
