package com.bank.corebanking_prod.feature.payment.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_idempotency", columnNames = {"principal_id", "operation", "idempotency_key"}),
                @UniqueConstraint(name = "uk_payment_idempotency_history", columnNames = {"transaction_history_id"})
        }
)
public class PaymentIdempotencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Zero is reserved for trusted internal/legacy calls; authenticated API calls use their identity id. */
    @Column(name = "principal_id", nullable = false)
    private Long principalId = 0L;

    @Column(name = "operation", nullable = false, length = 40)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "response_payload", nullable = false, columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "transaction_history_id", nullable = false)
    private Long transactionHistoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public PaymentIdempotencyEntity() {}

    public PaymentIdempotencyEntity(String operation,
                                  String idempotencyKey,
                                  String requestHash,
                                  Integer responseStatus,
                                  String responsePayload,
                                  Long transactionHistoryId) {
        this(0L, operation, idempotencyKey, requestHash, responseStatus, responsePayload, transactionHistoryId);
    }

    public PaymentIdempotencyEntity(Long principalId,
                                  String operation,
                                  String idempotencyKey,
                                  String requestHash,
                                  Integer responseStatus,
                                  String responsePayload,
                                  Long transactionHistoryId) {
        this.principalId = principalId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responsePayload = responsePayload;
        this.transactionHistoryId = transactionHistoryId;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.completedAt == null) {
            this.completedAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(Long principalId) {
        this.principalId = principalId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public Long getTransactionHistoryId() {
        return transactionHistoryId;
    }

    public void setTransactionHistoryId(Long transactionHistoryId) {
        this.transactionHistoryId = transactionHistoryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
