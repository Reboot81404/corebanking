package com.bank.corebanking_prod.feature.account.controller;

import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BANK_OPERATOR', 'ADMIN')")
    public ResponseEntity<AccountDto.Response> createAccount(
            @Valid @RequestBody AccountDto.Create request) {
        AccountDto.Response response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@resourceOwnershipService.canAccessAccount(#id)")
    public ResponseEntity<AccountDto.Response> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping("/iban/{iban}")
    @PreAuthorize("@resourceOwnershipService.canAccessAccountByIban(#iban)")
    public ResponseEntity<AccountDto.Response> getAccountByIban(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.getAccountByIban(iban));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("@resourceOwnershipService.canListAccountsForCustomer(#customerId)")
    public ResponseEntity<Page<AccountDto.Response>> getAccountsByCustomerId(
            @PathVariable Long customerId,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId, pageable));
    }
}
