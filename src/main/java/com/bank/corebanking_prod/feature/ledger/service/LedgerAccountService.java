package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;

public interface LedgerAccountService {

    LedgerAccountEntity getOrCreateCustomerDepositAccount(Long bankAccountId, String iban, Currency currency);

    LedgerAccountEntity getCustomerDepositAccount(Long bankAccountId);

    LedgerAccountEntity getClearingAccount(Currency currency);
}
