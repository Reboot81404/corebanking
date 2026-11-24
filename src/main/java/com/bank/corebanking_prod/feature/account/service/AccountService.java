package com.bank.corebanking_prod.feature.account.service;

import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    AccountDto.Response createAccount(AccountDto.Create request);

    AccountDto.Response getAccountById(Long id);

    AccountDto.Response getAccountByIban(String iban);

    Page<AccountDto.Response> getAccountsByCustomerId(Long customerId, Pageable pageable);

    java.util.List<AccountDto.Response> getAllAccounts();

    boolean accountBelongsToCustomer(Long accountId, Long customerId);

    boolean accountWithIbanBelongsToCustomer(String iban, Long customerId);

    Long findAccountIdByIban(String iban);
}
