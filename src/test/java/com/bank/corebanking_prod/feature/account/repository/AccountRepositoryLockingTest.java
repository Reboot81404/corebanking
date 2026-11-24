package com.bank.corebanking_prod.feature.account.repository;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TASK-001: findByIbanForUpdate (PESSIMISTIC_WRITE) sorgu doğrulaması.
 *
 * <p>Bu testler H2 (in-memory) test veritabanında çalışır.
 * H2, {@code SELECT ... FOR UPDATE} sözdizimini destekler ve sorgunun
 * doğru entity'yi döndürdüğünü doğrulamak için yeterlidir.
 * Ancak gerçek pessimistic lock davranışı ve concurrency garantileri
 * yalnızca hedef veritabanı PostgreSQL üzerinde doğrulanabilir.</p>
 */
@DataJpaTest
class AccountRepositoryLockingTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private AccountEntity savedAccount;

    private static final String TEST_IBAN = "TR000000000000000000000001";
    private static final String NON_EXISTENT_IBAN = "TR999999999999999999999999";

    @BeforeEach
    void setUp() {
        CustomerEntity customer = new CustomerEntity(
                "10000000146",
                "Lock",
                "Test",
                "locktest@example.com",
                "05551234567"
        );
        customer = customerRepository.saveAndFlush(customer);

        AccountEntity account = new AccountEntity();
        account.setCustomer(customer);
        account.setIban(TEST_IBAN);
        account.setBalance(new BigDecimal("1000.00"));
        account.setCurrency(Currency.TRY);
        savedAccount = accountRepository.saveAndFlush(account);
    }

    @Test
    @DisplayName("findByIbanForUpdate – mevcut IBAN için doğru hesabı döndürür [H2]")
    void shouldReturnAccountWhenIbanExists() {
        Optional<AccountEntity> result = accountRepository.findByIbanForUpdate(TEST_IBAN);

        assertTrue(result.isPresent(), "Mevcut IBAN için hesap döndürülmeliydi");
        assertEquals(savedAccount.getId(), result.get().getId());
        assertEquals(TEST_IBAN, result.get().getIban());
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.get().getBalance()));
        assertEquals(Currency.TRY, result.get().getCurrency());
    }

    @Test
    @DisplayName("findByIbanForUpdate – olmayan IBAN için Optional.empty() döndürür [H2]")
    void shouldReturnEmptyWhenIbanDoesNotExist() {
        Optional<AccountEntity> result = accountRepository.findByIbanForUpdate(NON_EXISTENT_IBAN);

        assertFalse(result.isPresent(), "Olmayan IBAN için Optional.empty() döndürülmeliydi");
    }

    @Test
    @DisplayName("findByIbanForUpdate – mevcut findByIban ile aynı entity'yi döndürür [H2]")
    void shouldReturnSameEntityAsFindByIban() {
        Optional<AccountEntity> lockResult = accountRepository.findByIbanForUpdate(TEST_IBAN);
        Optional<AccountEntity> normalResult = accountRepository.findByIban(TEST_IBAN);

        assertTrue(lockResult.isPresent());
        assertTrue(normalResult.isPresent());
        assertEquals(normalResult.get().getId(), lockResult.get().getId());
        assertEquals(normalResult.get().getIban(), lockResult.get().getIban());
    }
}
