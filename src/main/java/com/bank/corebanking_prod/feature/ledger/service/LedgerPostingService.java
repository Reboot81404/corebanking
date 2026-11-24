package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;

import java.math.BigDecimal;

public interface LedgerPostingService {

    JournalEntryEntity postInternalTransfer(
            Long transactionHistoryId,
            Long senderAccountId,
            Long receiverAccountId,
            BigDecimal amount,
            Currency currency
    );
}
