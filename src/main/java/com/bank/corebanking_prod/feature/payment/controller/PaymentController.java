package com.bank.corebanking_prod.feature.payment.controller;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.auth.security.AuthenticatedUser;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/transfer")
    @PreAuthorize("@resourceOwnershipService.canInitiateTransferFrom(#request.senderIban)")
    public ResponseEntity<TransactionHistoryDto.TransferResponse> transferMoney(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransactionHistoryDto.TransferRequest request,
            Authentication authentication) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException("Idempotency-Key header is required.");
        }

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                || user.getId() == null || user.getId() <= 0) {
            throw new AccessDeniedException("An authenticated identity is required for transfer processing.");
        }

        TransactionHistoryDto.TransferResponse response = paymentService.transferMoney(
                request, idempotencyKey.trim(), user.getId());
        return ResponseEntity.ok(response);
    }
}
