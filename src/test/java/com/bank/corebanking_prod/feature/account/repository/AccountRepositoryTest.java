package com.bank.corebanking_prod.feature.account.repository;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldEnforceUniqueIbanAtDatabaseLevel() {
        CustomerEntity customer = createCustomer();

        AccountEntity firstAccount = createAccount(customer, "TR710000000100000000000000");
        accountRepository.saveAndFlush(firstAccount);

        AccountEntity secondAccount = createAccount(customer, "TR710000000100000000000000");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> accountRepository.saveAndFlush(secondAccount)
        );
    }

    @Test
    void shouldReturnCustomerAccountsAsPage() {
        CustomerEntity customer = createCustomer();

        for (int i = 1; i <= 3; i++) {
            String iban = String.format("TR%024d", i);
            accountRepository.save(createAccount(customer, iban));
        }
        accountRepository.flush();

        Page<AccountEntity> firstPage = accountRepository.findByCustomerId(
                customer.getId(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "id"))
        );

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getNumberOfElements());
        assertEquals(2, firstPage.getTotalPages());
    }

    private CustomerEntity createCustomer() {
        CustomerEntity customer = new CustomerEntity(
                "10000000146",
                "Test",
                "Customer",
                "test@example.com",
                "05551234567"
        );
        return customerRepository.saveAndFlush(customer);
    }

    private AccountEntity createAccount(CustomerEntity customer, String iban) {
        AccountEntity account = new AccountEntity();
        account.setCustomer(customer);
        account.setIban(iban);
        account.setBalance(new BigDecimal("100.00"));
        account.setCurrency(Currency.TRY);
        return account;
    }
}
