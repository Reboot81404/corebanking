package com.bank.corebanking_prod.feature.payment.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.common.exception.IdempotencyConflictException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerPostingService;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.model.entity.PaymentIdempotencyEntity;
import com.bank.corebanking_prod.feature.payment.model.entity.TransactionHistoryEntity;
import com.bank.corebanking_prod.feature.payment.repository.PaymentIdempotencyRepository;
import com.bank.corebanking_prod.feature.payment.repository.TransactionHistoryRepository;
import com.bank.corebanking_prod.feature.payment.util.PayloadHashCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private PaymentIdempotencyRepository paymentIdempotencyRepository;

    @Mock
    private LedgerPostingService ledgerPostingService;

    @Spy
    private PayloadHashCalculator payloadHashCalculator = new PayloadHashCalculator();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PaymentService paymentService;

    private static final String SENDER_IBAN = "TR100000000000000000000001";
    private static final String RECEIVER_IBAN = "TR200000000000000000000002";
    private static final String VALID_KEY = "test-idempotency-key-12345";

    private AccountEntity senderAccount;
    private AccountEntity receiverAccount;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                accountRepository,
                transactionHistoryRepository,
                paymentIdempotencyRepository,
                payloadHashCalculator,
                objectMapper,
                ledgerPostingService
        );

        senderAccount = new AccountEntity();
        senderAccount.setId(1L);
        senderAccount.setIban(SENDER_IBAN);
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setCurrency(Currency.TRY);

        receiverAccount = new AccountEntity();
        receiverAccount.setId(2L);
        receiverAccount.setIban(RECEIVER_IBAN);
        receiverAccount.setBalance(new BigDecimal("500.00"));
        receiverAccount.setCurrency(Currency.TRY);
    }

    @Test
    @DisplayName("Başarılı transfer: Bakiyeler güncellenmeli, history ve idempotency kaydedilmeli")
    void shouldTransferMoneySuccessfully() {
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIbanForUpdate(RECEIVER_IBAN)).thenReturn(Optional.of(receiverAccount));
        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("200.00")
        );

        TransactionHistoryDto.TransferResponse response = paymentService.transferMoney(request, VALID_KEY);

        assertNotNull(response);
        assertEquals("Transfer is completed.", response.getMessage());
        assertEquals(SENDER_IBAN, response.getSenderIban());
        assertEquals(RECEIVER_IBAN, response.getReceiverIban());
        assertEquals(new BigDecimal("200.00"), response.getAmount());

        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());

        verify(accountRepository).save(senderAccount);
        verify(accountRepository).save(receiverAccount);
        verify(transactionHistoryRepository).save(any());
        verify(ledgerPostingService).postInternalTransfer(any(), eq(1L), eq(2L), eq(new BigDecimal("200.00")), eq(Currency.TRY));
        verify(paymentIdempotencyRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("Ledger kaydı başarısız olduğunda hata fırlatılmalı ve idempotency kaydedilmemeli")
    void shouldFailWhenLedgerPostingFails() {
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIbanForUpdate(RECEIVER_IBAN)).thenReturn(Optional.of(receiverAccount));
        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());
        doThrow(new BusinessException("Ledger error")).when(ledgerPostingService).postInternalTransfer(any(), any(), any(), any(), any());

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("200.00")
        );

        assertThrows(BusinessException.class, () -> paymentService.transferMoney(request, VALID_KEY));
        verify(paymentIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("İdempotency: Aynı key ve aynı payload tekrar geldiğinde önceki sonuç dönmeli, hesaplar kilitlenmemeli")
    void shouldReturnCachedResponseWhenSameKeyAndPayload() throws Exception {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("150.00")
        );

        String hash = payloadHashCalculator.calculateHash(request);
        TransactionHistoryDto.TransferResponse cachedResp = new TransactionHistoryDto.TransferResponse(
                "Transfer is completed.",
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("150.00"),
                LocalDateTime.now()
        );
        String json = objectMapper.writeValueAsString(cachedResp);

        PaymentIdempotencyEntity existingRecord = new PaymentIdempotencyEntity(
                PayloadHashCalculator.OPERATION_TRANSFER_V1,
                VALID_KEY,
                hash,
                200,
                json,
                99L
        );

        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(
                0L,
                PayloadHashCalculator.OPERATION_TRANSFER_V1,
                VALID_KEY
        )).thenReturn(Optional.of(existingRecord));

        TransactionHistoryDto.TransferResponse response = paymentService.transferMoney(request, VALID_KEY);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.00"), response.getAmount());

        // Hesap kilitlerine ve transfer history'ye ASLA gidilmemeli
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionHistoryRepository);
    }

    @Test
    @DisplayName("İdempotency çakışması: Aynı key farklı payload ile çağrıldığında 409 fırlatılmalı")
    void shouldThrowConflictWhenSameKeyDifferentPayload() {
        TransactionHistoryDto.TransferRequest originalReq = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );
        String originalHash = payloadHashCalculator.calculateHash(originalReq);

        PaymentIdempotencyEntity existingRecord = new PaymentIdempotencyEntity(
                PayloadHashCalculator.OPERATION_TRANSFER_V1,
                VALID_KEY,
                originalHash,
                200,
                "{}",
                1L
        );

        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(
                0L,
                PayloadHashCalculator.OPERATION_TRANSFER_V1,
                VALID_KEY
        )).thenReturn(Optional.of(existingRecord));

        // Farklı tutar ile aynı key deneniyor
        TransactionHistoryDto.TransferRequest differentReq = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("200.00")
        );

        assertThrows(
                IdempotencyConflictException.class,
                () -> paymentService.transferMoney(differentReq, VALID_KEY)
        );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionHistoryRepository);
    }

    @Test
    @DisplayName("Geçersiz key formatında BusinessException fırlatılmalı")
    void shouldThrowExceptionWhenInvalidKey() {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        assertThrows(
                BusinessException.class,
                () -> paymentService.transferMoney(request, "short")
        );
    }

    @Test
    @DisplayName("Deterministik sıra testi: Ters yönlü transferde kilitler yine alfabetik sırada alınmalı")
    void shouldAcquireLocksInDeterministicOrderWhenReversed() {
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIbanForUpdate(RECEIVER_IBAN)).thenReturn(Optional.of(receiverAccount));
        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                RECEIVER_IBAN,
                SENDER_IBAN,
                new BigDecimal("100.00")
        );

        TransactionHistoryDto.TransferResponse response = paymentService.transferMoney(request, VALID_KEY);

        assertNotNull(response);

        InOrder inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIbanForUpdate(SENDER_IBAN);
        inOrder.verify(accountRepository).findByIbanForUpdate(RECEIVER_IBAN);
    }

    @Test
    @DisplayName("Yetersiz bakiye durumunda BusinessException fırlatılmalı ve bakiye değişmemeli")
    void shouldThrowExceptionWhenInsufficientBalance() {
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIbanForUpdate(RECEIVER_IBAN)).thenReturn(Optional.of(receiverAccount));
        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("1500.00")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.transferMoney(request, VALID_KEY)
        );

        assertEquals("Insufficient account balance.", exception.getMessage());
        assertEquals(new BigDecimal("1000.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), receiverAccount.getBalance());

        verify(accountRepository, never()).save(any());
        verify(transactionHistoryRepository, never()).save(any());
        verify(paymentIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Tutar sıfır veya negatif olduğunda BusinessException fırlatılmalı")
    void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
        TransactionHistoryDto.TransferRequest zeroRequest = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                BigDecimal.ZERO
        );

        assertThrows(
                BusinessException.class,
                () -> paymentService.transferMoney(zeroRequest, VALID_KEY)
        );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionHistoryRepository);
    }

    @Test
    @DisplayName("Gönderen ve alıcı IBAN aynı olduğunda BusinessException fırlatılmalı")
    void shouldThrowExceptionWhenSenderAndReceiverAreSame() {
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                SENDER_IBAN,
                new BigDecimal("100.00")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.transferMoney(request, VALID_KEY)
        );

        assertEquals("Sender and receiver accounts cannot be the same.", exception.getMessage());
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(transactionHistoryRepository);
    }

    @Test
    @DisplayName("Gönderen hesap bulunamadığında ResourceNotFoundException fırlatılmalı")
    void shouldThrowExceptionWhenSenderNotFound() {
        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.empty());

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.transferMoney(request, VALID_KEY)
        );

        assertEquals("Sender account not found.", exception.getMessage());
        verify(transactionHistoryRepository, never()).save(any());
        verify(paymentIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Farklı para birimleri arasında transfere izin verilmemeli")
    void shouldThrowExceptionWhenCurrenciesAreDifferent() {
        receiverAccount.setCurrency(Currency.USD);

        when(paymentIdempotencyRepository.findByPrincipalIdAndOperationAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByIbanForUpdate(SENDER_IBAN)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIbanForUpdate(RECEIVER_IBAN)).thenReturn(Optional.of(receiverAccount));

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                SENDER_IBAN,
                RECEIVER_IBAN,
                new BigDecimal("100.00")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.transferMoney(request, VALID_KEY)
        );

        assertEquals("Transfers between different currencies are not allowed.", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionHistoryRepository, never()).save(any());
        verify(paymentIdempotencyRepository, never()).saveAndFlush(any());
    }
}
