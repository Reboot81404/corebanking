package com.bank.corebanking_prod.feature.payment.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.common.exception.IdempotencyConflictException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerPostingService;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.model.entity.PaymentIdempotencyEntity;
import com.bank.corebanking_prod.feature.payment.model.entity.TransactionHistoryEntity;
import com.bank.corebanking_prod.feature.payment.repository.PaymentIdempotencyRepository;
import com.bank.corebanking_prod.feature.payment.repository.TransactionHistoryRepository;
import com.bank.corebanking_prod.feature.payment.util.PayloadHashCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    /** Reserved scope for trusted internal calls and idempotency rows created before authentication was introduced. */
    private static final long SYSTEM_PRINCIPAL_ID = 0L;

    private final AccountRepository accountRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PaymentIdempotencyRepository paymentIdempotencyRepository;
    private final PayloadHashCalculator payloadHashCalculator;
    private final ObjectMapper objectMapper;
    private final LedgerPostingService ledgerPostingService;

    public PaymentService(
            AccountRepository accountRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            PaymentIdempotencyRepository paymentIdempotencyRepository,
            PayloadHashCalculator payloadHashCalculator,
            ObjectMapper objectMapper,
            LedgerPostingService ledgerPostingService) {

        this.accountRepository = accountRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.paymentIdempotencyRepository = paymentIdempotencyRepository;
        this.payloadHashCalculator = payloadHashCalculator;
        this.objectMapper = objectMapper;
        this.ledgerPostingService = ledgerPostingService;
    }

    @Transactional
    public TransactionHistoryDto.TransferResponse transferMoney(
            TransactionHistoryDto.TransferRequest request) {
        return transferMoney(request, UUID.randomUUID().toString(), SYSTEM_PRINCIPAL_ID);
    }

    /** Trusted internal entry point; HTTP requests must use the overload carrying the authenticated identity id. */
    @Transactional
    public TransactionHistoryDto.TransferResponse transferMoney(
            TransactionHistoryDto.TransferRequest request,
            String idempotencyKey) {
        return transferMoney(request, idempotencyKey, SYSTEM_PRINCIPAL_ID);
    }

    @Transactional
    public TransactionHistoryDto.TransferResponse transferMoney(
            TransactionHistoryDto.TransferRequest request,
            String idempotencyKey,
            Long principalId) {

        validatePrincipalId(principalId);
        payloadHashCalculator.validateKey(idempotencyKey);
        String requestHash = payloadHashCalculator.calculateHash(request);

        // 1. Hesap kilitlerinden ÖNCE idempotency kontrolü
        Optional<PaymentIdempotencyEntity> existing =
                paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(
                        principalId,
                        PayloadHashCalculator.OPERATION_TRANSFER_V1,
                        idempotencyKey
                );

        if (existing.isPresent()) {
            return handleExistingRecord(existing.get(), requestHash);
        }

        // 2. Temel kontroller
        BigDecimal amount = request.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Transfer amount must be greater than zero."
            );
        }

        if (request.getSenderIban().equals(request.getReceiverIban())) {
            throw new BusinessException(
                    "Sender and receiver accounts cannot be the same."
            );
        }

        // 3. Deterministik sırada hesapları kilitle (SPEC-001)
        String senderIban = request.getSenderIban();
        String receiverIban = request.getReceiverIban();

        String firstIban = senderIban.compareTo(receiverIban) < 0 ? senderIban : receiverIban;
        String secondIban = senderIban.compareTo(receiverIban) < 0 ? receiverIban : senderIban;

        AccountEntity firstAccount = accountRepository.findByIbanForUpdate(firstIban)
                .orElseThrow(() -> new ResourceNotFoundException(
                        firstIban.equals(senderIban) ? "Sender account not found." : "Receiver account not found."
                ));

        AccountEntity secondAccount = accountRepository.findByIbanForUpdate(secondIban)
                .orElseThrow(() -> new ResourceNotFoundException(
                        secondIban.equals(senderIban) ? "Sender account not found." : "Receiver account not found."
                ));

        AccountEntity senderAccount = firstIban.equals(senderIban) ? firstAccount : secondAccount;
        AccountEntity receiverAccount = firstIban.equals(senderIban) ? secondAccount : firstAccount;

        if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            throw new BusinessException(
                    "Transfers between different currencies are not allowed."
            );
        }

        if (senderAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "Insufficient account balance."
            );
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        TransactionHistoryEntity history = new TransactionHistoryEntity(
                senderAccount.getIban(),
                receiverAccount.getIban(),
                amount,
                senderAccount.getCurrency()
        );

        transactionHistoryRepository.save(history);

        // 4. Ledger posting: Aynı transaction içinde çift taraflı muhasebe kaydı oluştur (SPEC-005)
        ledgerPostingService.postInternalTransfer(
                history.getId(),
                senderAccount.getId(),
                receiverAccount.getId(),
                amount,
                senderAccount.getCurrency()
        );

        TransactionHistoryDto.TransferResponse response = new TransactionHistoryDto.TransferResponse(
                "Transfer is completed.",
                senderAccount.getIban(),
                receiverAccount.getIban(),
                amount,
                history.getTimestamp()
        );

        // 5. Idempotency kaydını aynı transaction içinde kaydet
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            PaymentIdempotencyEntity idempotencyEntity = new PaymentIdempotencyEntity(
                    principalId,
                    PayloadHashCalculator.OPERATION_TRANSFER_V1,
                    idempotencyKey,
                    requestHash,
                    200,
                    responseJson,
                    history.getId()
            );
            paymentIdempotencyRepository.saveAndFlush(idempotencyEntity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response", e);
        }

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public TransactionHistoryDto.TransferResponse resolveDuplicateInNewTransaction(
            TransactionHistoryDto.TransferRequest request,
            String idempotencyKey) {
        return resolveDuplicateInNewTransaction(request, idempotencyKey, SYSTEM_PRINCIPAL_ID);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public TransactionHistoryDto.TransferResponse resolveDuplicateInNewTransaction(
            TransactionHistoryDto.TransferRequest request,
            String idempotencyKey,
            Long principalId) {

        validatePrincipalId(principalId);
        String requestHash = payloadHashCalculator.calculateHash(request);

        PaymentIdempotencyEntity existing = paymentIdempotencyRepository
                .findByPrincipalIdAndOperationAndIdempotencyKey(
                        principalId,
                        PayloadHashCalculator.OPERATION_TRANSFER_V1,
                        idempotencyKey
                )
                .orElseThrow(() -> new IdempotencyConflictException(
                        "Concurrent transfer conflict. Please retry your request."
                ));

        return handleExistingRecord(existing, requestHash);
    }

    private void validatePrincipalId(Long principalId) {
        if (principalId == null || principalId < SYSTEM_PRINCIPAL_ID) {
            throw new IllegalArgumentException("A valid idempotency principal scope is required.");
        }
    }

    private TransactionHistoryDto.TransferResponse handleExistingRecord(
            PaymentIdempotencyEntity record,
            String requestHash) {

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "An existing transfer exists with the same Idempotency-Key but different request parameters."
            );
        }

        try {
            return objectMapper.readValue(
                    record.getResponsePayload(),
                    TransactionHistoryDto.TransferResponse.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached response", e);
        }
    }
}
