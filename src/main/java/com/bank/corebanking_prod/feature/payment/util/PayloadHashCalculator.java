package com.bank.corebanking_prod.feature.payment.util;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Component
public class PayloadHashCalculator {

    public static final String OPERATION_TRANSFER_V1 = "PAYMENT_TRANSFER_V1";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    public void validateKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key.trim()).matches()) {
            throw new BusinessException(
                    "Invalid Idempotency-Key. Key must be between 16 and 64 alphanumeric characters, underscores or hyphens."
            );
        }
    }

    public String calculateHash(TransactionHistoryDto.TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        String senderIban = request.getSenderIban() != null
                ? request.getSenderIban().trim().toUpperCase()
                : "";
        String receiverIban = request.getReceiverIban() != null
                ? request.getReceiverIban().trim().toUpperCase()
                : "";

        String normalizedAmount = normalizeAmount(request.getAmount());

        String canonicalPayload = String.join("|",
                OPERATION_TRANSFER_V1,
                senderIban,
                receiverIban,
                normalizedAmount
        );

        return sha256Hex(canonicalPayload);
    }

    public String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
