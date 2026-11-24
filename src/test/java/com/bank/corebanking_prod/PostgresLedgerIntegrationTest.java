package com.bank.corebanking_prod;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import com.bank.corebanking_prod.feature.ledger.model.dto.ReconciliationReportDto;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryLineEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryStatus;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryType;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryLineRepository;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerAccountService;
import com.bank.corebanking_prod.feature.ledger.service.LedgerReconciliationService;
import com.bank.corebanking_prod.feature.payment.model.dto.TransactionHistoryDto;
import com.bank.corebanking_prod.feature.payment.model.entity.TransactionHistoryEntity;
import com.bank.corebanking_prod.feature.payment.repository.TransactionHistoryRepository;
import com.bank.corebanking_prod.feature.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
class PostgresLedgerIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Autowired
    private LedgerReconciliationService reconciliationService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalEntryLineRepository journalEntryLineRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    private AccountEntity senderAccount;
    private AccountEntity receiverAccount;

    @BeforeEach
    void setUp() {
        CustomerEntity customer = customerRepository.findByTckn("10000000146")
                .orElseGet(() -> customerRepository.save(new CustomerEntity(
                        "10000000146",
                        "Ledger",
                        "Test",
                        "ledger.test@example.com",
                        "05551234567"
                )));

        String senderIban = "TR910000000100000000000001";
        senderAccount = accountRepository.findByIban(senderIban)
                .orElseGet(() -> {
                    AccountEntity acc = new AccountEntity();
                    acc.setCustomer(customer);
                    acc.setIban(senderIban);
                    acc.setCurrency(Currency.TRY);
                    acc.setBalance(new BigDecimal("10000.00"));
                    AccountEntity saved = accountRepository.save(acc);
                    ledgerAccountService.getOrCreateCustomerDepositAccount(saved.getId(), saved.getIban(), saved.getCurrency());
                    return saved;
                });

        // Ensure sender balance is funded for tests
        if (senderAccount.getBalance().compareTo(new BigDecimal("1000.00")) < 0) {
            senderAccount.setBalance(new BigDecimal("10000.00"));
            accountRepository.save(senderAccount);
        }
        ledgerAccountService.getOrCreateCustomerDepositAccount(senderAccount.getId(), senderAccount.getIban(), senderAccount.getCurrency());

        String receiverIban = "TR910000000100000000000002";
        receiverAccount = accountRepository.findByIban(receiverIban)
                .orElseGet(() -> {
                    AccountEntity acc = new AccountEntity();
                    acc.setCustomer(customer);
                    acc.setIban(receiverIban);
                    acc.setCurrency(Currency.TRY);
                    acc.setBalance(new BigDecimal("500.00"));
                    AccountEntity saved = accountRepository.save(acc);
                    ledgerAccountService.getOrCreateCustomerDepositAccount(saved.getId(), saved.getIban(), saved.getCurrency());
                    return saved;
                });
        ledgerAccountService.getOrCreateCustomerDepositAccount(receiverAccount.getId(), receiverAccount.getIban(), receiverAccount.getCurrency());
    }

    @Test
    @DisplayName("Internal transfer sonrası PostgreSQL üzerinde dengeli journal entry ve 2 satır oluşmalı")
    void shouldCreateBalancedJournalEntryOnTransfer() {
        String key = "ledger-test-key-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("150.00");

        TransactionHistoryDto.TransferRequest request = new TransactionHistoryDto.TransferRequest(
                senderAccount.getIban(),
                receiverAccount.getIban(),
                transferAmount
        );

        TransactionHistoryDto.TransferResponse response = paymentService.transferMoney(request, key);
        assertNotNull(response);

        // Find the transaction history record
        List<TransactionHistoryEntity> histories = transactionHistoryRepository.findAll().stream()
                .filter(h -> h.getSenderIban().equals(senderAccount.getIban()) && h.getReceiverIban().equals(receiverAccount.getIban()))
                .toList();
        assertFalse(histories.isEmpty());
        TransactionHistoryEntity latestHistory = histories.get(histories.size() - 1);

        // Verify Journal Entry exists
        JournalEntryEntity entry = journalEntryRepository.findByTransactionHistoryId(latestHistory.getId())
                .orElse(null);
        assertNotNull(entry, "Journal entry must exist for transaction history");
        assertEquals(JournalEntryType.INTERNAL_TRANSFER, entry.getTransactionType());
        assertEquals(JournalEntryStatus.POSTED, entry.getStatus());
        assertEquals(Currency.TRY, entry.getCurrency());

        // Verify Journal Lines
        List<JournalEntryLineEntity> lines = journalEntryLineRepository.findByJournalEntryId(entry.getId());
        assertEquals(2, lines.size(), "Entry must contain exactly 2 lines");

        BigDecimal totalDebit = lines.stream().map(JournalEntryLineEntity::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalEntryLineEntity::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(transferAmount, totalDebit);
        assertEquals(transferAmount, totalCredit);
        assertEquals(totalDebit, totalCredit, "Double-entry invariant: Total Debit must equal Total Credit");

        // Idempotency check: same key must not create another journal entry
        TransactionHistoryDto.TransferResponse cached = paymentService.transferMoney(request, key);
        assertNotNull(cached);

        long journalCount = journalEntryRepository.findAll().stream()
                .filter(j -> j.getTransactionHistoryId() != null && j.getTransactionHistoryId().equals(latestHistory.getId()))
                .count();
        assertEquals(1, journalCount, "Duplicate request must not create second journal entry");
    }

    @Test
    @DisplayName("PostgreSQL üzerinde reconciliation raporu başarıyla üretilebilmeli")
    void shouldGenerateReconciliationReport() {
        ReconciliationReportDto report = reconciliationService.reconcileCustomerDeposits();
        assertNotNull(report);
        assertTrue(report.getTotalAccountsChecked() > 0);
        assertNotNull(report.getTimestamp());
    }
}
