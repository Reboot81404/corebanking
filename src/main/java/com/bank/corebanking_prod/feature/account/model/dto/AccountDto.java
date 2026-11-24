
package com.bank.corebanking_prod.feature.account.model.dto;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDto {

    private AccountDto() {}

    public static class Create {

        @NotNull(message = "Customer ID is required.")
        private Long customerId;

        @NotNull(message = "Currency is required.")
        private Currency currency;

        @DecimalMin(value = "0.00", message = "Initial balance must be zero.")
        @DecimalMax(value = "0.00", message = "Initial balance must be zero.")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Balance can have maximum 17 integer digits and 2 decimal places."
        )
        private BigDecimal balance;

        public Create() {}

        public Create(
                Long customerId,
                Currency currency,
                BigDecimal balance) {

            this.customerId = customerId;
            this.currency = currency;
            this.balance = balance;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }

    public static class Response {

        private Long id;
        private String iban;
        private BigDecimal balance;
        private Currency currency;
        private Long customerId;
        private LocalDateTime createdAt;

        public Response() {}

        public Response(
                Long id,
                String iban,
                BigDecimal balance,
                Currency currency,
                Long customerId,
                LocalDateTime createdAt) {

            this.id = id;
            this.iban = iban;
            this.balance = balance;
            this.currency = currency;
            this.customerId = customerId;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIban() {
            return iban;
        }

        public void setIban(String iban) {
            this.iban = iban;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
