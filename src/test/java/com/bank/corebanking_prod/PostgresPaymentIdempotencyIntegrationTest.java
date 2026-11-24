package com.bank.corebanking_prod;

import com.bank.corebanking_prod.common.exception.IdempotencyConflictException;
import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerAccountService;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.repository.PaymentIdempotencyRepository;
import com.bank.corebanking_prod.feature.payment.repository.TransactionHistoryRepository;
import com.bank.corebanking_prod.feature.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=${DB_URL:jdbc:postgresql://192.168.1.221:5432/corebanking_prod}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=${DB_USERNAME:postgres}",
        "spring.datasource.password=${DB_PASSWORD:postgres}",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresPaymentIdempotencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private PaymentIdempotencyRepository paymentIdempotencyRepository;

    @Autowired
    private LedgerAccountService ledgerAccountService;

    private String senderIban;
    private String receiverIban;

    @BeforeEach
    void setUp() {
        CustomerEntity customer = customerRepository.findByTckn("10000000146")
                .orElseGet(() -> customerRepository.saveAndFlush(new CustomerEntity(
                        "10000000146",
                        "Idem",
                        "Tester",
                        "idemtest" + System.currentTimeMillis() + "@example.com",
                        "05551234567"
                )));

        senderIban = "TR" + String.format("%024d", System.nanoTime() % 100000000000L);
        AccountEntity sender = new AccountEntity();
        sender.setCustomer(customer);
        sender.setIban(senderIban);
        sender.setBalance(new BigDecimal("1000.00"));
        sender.setCurrency(Currency.TRY);
        accountRepository.saveAndFlush(sender);
        ledgerAccountService.getOrCreateCustomerDepositAccount(sender.getId(), sender.getIban(), sender.getCurrency());

        receiverIban = "TR" + String.format("%024d", (System.nanoTime() + 1) % 100000000000L);
        AccountEntity receiver = new AccountEntity();
        receiver.setCustomer(customer);
        receiver.setIban(receiverIban);
        receiver.setBalance(new BigDecimal("500.00"));
        receiver.setCurrency(Currency.TRY);
        accountRepository.saveAndFlush(receiver);
        ledgerAccountService.getOrCreateCustomerDepositAccount(receiver.getId(), receiver.getIban(), receiver.getCurrency());
    }

    @Test
    @DisplayName("PostgreSQL: Aynı Idempotency-Key ile eşzamanlı iki istek gönderildiğinde bakiye sadece bir kez düşmeli")
    void shouldExecuteOnlyOnceUnderConcurrentRequestsOnPostgres() throws Exception {
        String idempotencyKey = "concurrent-key-" + UUID.randomUUID();
        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                senderIban,
                receiverIban,
                new BigDecimal("300.00")
        );

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Callable<TransactionHistoryDto.TransferResponse> task = () -> {
            startLatch.await();
            try {
                try {
                    return paymentService.transferMoney(request, idempotencyKey);
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    return paymentService.resolveDuplicateInNewTransaction(request, idempotencyKey);
                }
            } finally {
                doneLatch.countDown();
            }
        };

        Future<TransactionHistoryDto.TransferResponse> future1 = executor.submit(task);
        Future<TransactionHistoryDto.TransferResponse> future2 = executor.submit(task);

        startLatch.countDown(); // İki thread aynı anda başlar
        doneLatch.await(10, TimeUnit.SECONDS);

        TransactionHistoryDto.TransferResponse resp1 = future1.get();
        TransactionHistoryDto.TransferResponse resp2 = future2.get();

        assertNotNull(resp1);
        assertNotNull(resp2);
        assertEquals(resp1.getAmount(), resp2.getAmount());

        AccountEntity senderAfter = accountRepository.findByIban(senderIban).orElseThrow();
        AccountEntity receiverAfter = accountRepository.findByIban(receiverIban).orElseThrow();

        // 1000 - 300 = 700 olmalı, ASLA 400 olmamalı!
        assertEquals(0, new BigDecimal("700.00").compareTo(senderAfter.getBalance()), "Bakiye tam olarak bir kez düşmeliydi");
        assertEquals(0, new BigDecimal("800.00").compareTo(receiverAfter.getBalance()), "Alıcı bakiyesi tam olarak bir kez artmalıydı");

        executor.shutdown();
    }

    @Test
    @DisplayName("PostgreSQL: Aynı key farklı tutarla çağrıldığında IdempotencyConflictException fırlatılmalı")
    void shouldThrowConflictWhenKeyReusedWithDifferentPayloadOnPostgres() {
        String idempotencyKey = "conflict-key-" + UUID.randomUUID();
        TransactionHistoryDto.TransferRequest req1 = new TransactionHistoryDto.TransferRequest(
                senderIban,
                receiverIban,
                new BigDecimal("100.00")
        );
        paymentService.transferMoney(req1, idempotencyKey);

        TransactionHistoryDto.TransferRequest req2 = new TransactionHistoryDto.TransferRequest(
                senderIban,
                receiverIban,
                new BigDecimal("200.00")
        );

        assertThrows(IdempotencyConflictException.class, () -> paymentService.transferMoney(req2, idempotencyKey));
    }
}
