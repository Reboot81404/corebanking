package com.bank.corebanking_prod.feature.account.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private IbanGeneratorService ibanGeneratorService;

    @Mock
    private LedgerAccountService ledgerAccountService;

    @InjectMocks
    private AccountServiceImp accountService;

    private CustomerEntity mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = new CustomerEntity(
                "10000000146",
                "Ali",
                "Veli",
                "ali.veli@example.com",
                "05551234567"
        );
        mockCustomer.setId(10L);
    }

    @Test
    @DisplayName("Başlangıç bakiyesi belirtilmediğinde (null) hesap 0.00 bakiye ile açılmalı")
    void shouldCreateAccountWithZeroBalanceWhenBalanceOmitted() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(mockCustomer));
        when(ibanGeneratorService.generate()).thenReturn("TR123456789012345678901234");
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AccountDto.Create request = new AccountDto.Create();
        request.setCustomerId(10L);
        request.setCurrency(Currency.TRY);
        request.setBalance(null); // Omitted

        AccountDto.Response response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getBalance());

        ArgumentCaptor<AccountEntity> entityCaptor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(entityCaptor.capture());
        assertEquals(BigDecimal.ZERO, entityCaptor.getValue().getBalance());
        verify(ledgerAccountService).getOrCreateCustomerDepositAccount(eq(1L), eq("TR123456789012345678901234"), eq(Currency.TRY));
    }

    @Test
    @DisplayName("Başlangıç bakiyesi 0.00 verildiğinde hesap 0.00 bakiye ile açılmalı")
    void shouldCreateAccountWithZeroBalanceWhenZeroProvided() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(mockCustomer));
        when(ibanGeneratorService.generate()).thenReturn("TR123456789012345678901234");
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AccountDto.Create request = new AccountDto.Create(10L, Currency.TRY, new BigDecimal("0.00"));

        AccountDto.Response response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getBalance());

        ArgumentCaptor<AccountEntity> entityCaptor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(entityCaptor.capture());
        assertEquals(BigDecimal.ZERO, entityCaptor.getValue().getBalance());
        verify(ledgerAccountService).getOrCreateCustomerDepositAccount(eq(1L), eq("TR123456789012345678901234"), eq(Currency.TRY));
    }

    @Test
    @DisplayName("Servise pozitif başlangıç bakiyesi gelirse BusinessException fırlatılmalı ve hesap kaydedilmemeli")
    void shouldThrowExceptionWhenPositiveBalanceAttempted() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(mockCustomer));

        AccountDto.Create request = new AccountDto.Create(10L, Currency.TRY, new BigDecimal("100.00"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> accountService.createAccount(request)
        );

        assertEquals("Initial balance must be zero.", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Müşteri bulunamadığında ResourceNotFoundException fırlatılmalı")
    void shouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        AccountDto.Create request = new AccountDto.Create(99L, Currency.TRY, BigDecimal.ZERO);

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.createAccount(request)
        );

        verify(accountRepository, never()).save(any());
    }
}
