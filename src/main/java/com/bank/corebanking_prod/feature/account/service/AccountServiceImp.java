package com.bank.corebanking_prod.feature.account.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import com.bank.corebanking_prod.feature.account.repository.AccountRepository;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import com.bank.corebanking_prod.feature.ledger.service.LedgerAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class AccountServiceImp implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final IbanGeneratorService ibanGeneratorService;
    private final LedgerAccountService ledgerAccountService;

    public AccountServiceImp(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            IbanGeneratorService ibanGeneratorService,
            LedgerAccountService ledgerAccountService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.ibanGeneratorService = ibanGeneratorService;
        this.ledgerAccountService = ledgerAccountService;
    }

    @Override
    @Transactional
    public AccountDto.Response createAccount(AccountDto.Create request) {
        CustomerEntity customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()
                ));

        if (request.getBalance() != null && request.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Initial balance must be zero.");
        }

        AccountEntity account = new AccountEntity();
        account.setCustomer(customer);
        account.setCurrency(request.getCurrency());
        account.setBalance(BigDecimal.ZERO);

        // Sequence her çağrıda benzersiz bir değer verdiği için existsByIban döngüsü kullanılmaz.
        account.setIban(ibanGeneratorService.generate());

        AccountEntity savedAccount = accountRepository.save(account);

        // Yeni banka hesabı için CUSTOMER_DEPOSIT ledger hesabı oluştur (SPEC-005)
        ledgerAccountService.getOrCreateCustomerDepositAccount(
                savedAccount.getId(),
                savedAccount.getIban(),
                savedAccount.getCurrency()
        );

        return mapToResponse(savedAccount);
    }

    @Override
    public AccountDto.Response getAccountById(Long id) {
        AccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + id
                ));

        return mapToResponse(account);
    }

    @Override
    public AccountDto.Response getAccountByIban(String iban) {
        AccountEntity account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with IBAN: " + iban
                ));

        return mapToResponse(account);
    }

    @Override
    public Page<AccountDto.Response> getAccountsByCustomerId(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }

        return accountRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public boolean accountBelongsToCustomer(Long accountId, Long customerId) {
        if (accountId == null || customerId == null) {
            return false;
        }
        return accountRepository.findById(accountId)
                .map(account -> customerId.equals(account.getCustomer().getId()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean accountWithIbanBelongsToCustomer(String iban, Long customerId) {
        if (iban == null || customerId == null) {
            return false;
        }
        return accountRepository.findByIban(iban)
                .map(account -> customerId.equals(account.getCustomer().getId()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Long findAccountIdByIban(String iban) {
        if (iban == null) {
            return null;
        }
        return accountRepository.findByIban(iban)
                .map(AccountEntity::getId)
                .orElse(null);
    }

    private AccountDto.Response mapToResponse(AccountEntity entity) {
        return new AccountDto.Response(
                entity.getId(),
                entity.getIban(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getCustomer().getId(),
                entity.getCreatedAt()
        );
    }
}
