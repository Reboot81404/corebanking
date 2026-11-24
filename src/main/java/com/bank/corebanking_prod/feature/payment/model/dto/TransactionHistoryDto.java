
package com.bank.corebanking_prod.feature.payment.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryDto {

    private TransactionHistoryDto() {}

    public static class TransferRequest {

        @NotBlank(message = "Sender IBAN is required.")
        @Pattern(
                regexp = "^TR[0-9]{24}$",
                message = "Sender IBAN must have 26 characters and start with TR."
        )
        private String senderIban;

        @NotBlank(message = "Receiver IBAN is required.")
        @Pattern(
                regexp = "^TR[0-9]{24}$",
                message = "Receiver IBAN must have 26 characters and start with TR."
        )
        private String receiverIban;

        @NotNull(message = "Transfer amount is required.")
        @DecimalMin(
                value = "0.01",
                message = "Transfer amount must be at least 0.01."
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount can have maximum 17 integer digits and 2 decimal places."
        )
        private BigDecimal amount;

        public TransferRequest() {}

        public TransferRequest(
                String senderIban,
                String receiverIban,
                BigDecimal amount) {

            this.senderIban = senderIban;
            this.receiverIban = receiverIban;
            this.amount = amount;
        }

        public String getSenderIban() {
            return senderIban;
        }

        public void setSenderIban(String senderIban) {
            this.senderIban = senderIban;
        }

        public String getReceiverIban() {
            return receiverIban;
        }

        public void setReceiverIban(String receiverIban) {
            this.receiverIban = receiverIban;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class TransferResponse {

        private String message;
        private String senderIban;
        private String receiverIban;
        private BigDecimal amount;
        private LocalDateTime timestamp;

        public TransferResponse() {}

        public TransferResponse(
                String message,
                String senderIban,
                String receiverIban,
                BigDecimal amount,
                LocalDateTime timestamp) {

            this.message = message;
            this.senderIban = senderIban;
            this.receiverIban = receiverIban;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSenderIban() {
            return senderIban;
        }

        public void setSenderIban(String senderIban) {
            this.senderIban = senderIban;
        }

        public String getReceiverIban() {
            return receiverIban;
        }

        public void setReceiverIban(String receiverIban) {
            this.receiverIban = receiverIban;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}