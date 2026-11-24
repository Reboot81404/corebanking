package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import com.bank.corebanking_prod.feature.ledger.model.enums.NormalBalance;
import com.bank.corebanking_prod.feature.ledger.repository.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerAccountServiceImpl implements LedgerAccountService {

    private final LedgerAccountRepository ledgerAccountRepository;

    public LedgerAccountServiceImpl(LedgerAccountRepository ledgerAccountRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @Override
    @Transactional
    public LedgerAccountEntity getOrCreateCustomerDepositAccount(Long bankAccountId, String iban, Currency currency) {
        return ledgerAccountRepository.findByBankAccountId(bankAccountId)
                .orElseGet(() -> {
                    LedgerAccountEntity newAccount = new LedgerAccountEntity(
                            "DEP-" + iban,
                            "Customer Deposit - " + iban,
                            LedgerAccountType.CUSTOMER_DEPOSIT,
                            NormalBalance.CREDIT,
                            currency,
                            bankAccountId
                    );
                    return ledgerAccountRepository.save(newAccount);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccountEntity getCustomerDepositAccount(Long bankAccountId) {
        return ledgerAccountRepository.findByBankAccountId(bankAccountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer deposit ledger account not found for bank account id: " + bankAccountId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccountEntity getClearingAccount(Currency currency) {
        return ledgerAccountRepository.findByAccountTypeAndCurrency(
                LedgerAccountType.OPENING_BALANCE_CLEARING,
                currency
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Opening balance clearing ledger account not found for currency: " + currency
        ));
    }
}
