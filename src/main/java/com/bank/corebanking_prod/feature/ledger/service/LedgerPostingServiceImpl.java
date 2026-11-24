package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.common.exception.BusinessException;
import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.domain.LedgerDomainValidator;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryLineEntity;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryStatus;
import com.bank.corebanking_prod.feature.ledger.model.enums.JournalEntryType;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerPostingServiceImpl implements LedgerPostingService {

    private final LedgerAccountService ledgerAccountService;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerPostingServiceImpl(
            LedgerAccountService ledgerAccountService,
            JournalEntryRepository journalEntryRepository) {
        this.ledgerAccountService = ledgerAccountService;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    @Transactional
    public JournalEntryEntity postInternalTransfer(
            Long transactionHistoryId,
            Long senderAccountId,
            Long receiverAccountId,
            BigDecimal amount,
            Currency currency) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transfer amount must be strictly positive for ledger posting.");
        }

        if (transactionHistoryId != null && journalEntryRepository.existsByTransactionHistoryId(transactionHistoryId)) {
            throw new BusinessException("Journal entry already exists for transaction history id: " + transactionHistoryId);
        }

        LedgerAccountEntity senderLedgerAccount = ledgerAccountService.getCustomerDepositAccount(senderAccountId);
        LedgerAccountEntity receiverLedgerAccount = ledgerAccountService.getCustomerDepositAccount(receiverAccountId);

        if (senderLedgerAccount.getCurrency() != currency || receiverLedgerAccount.getCurrency() != currency) {
            throw new BusinessException("Ledger account currency does not match transfer currency.");
        }

        String referenceNumber = "JE-" + UUID.randomUUID();
        JournalEntryEntity entry = new JournalEntryEntity(
                referenceNumber,
                transactionHistoryId,
                JournalEntryType.INTERNAL_TRANSFER,
                currency,
                String.format("Internal transfer of %s %s from account %s to %s", amount, currency, senderAccountId, receiverAccountId),
                JournalEntryStatus.POSTED
        );

        // Sender: DEBIT customer deposit liability
        JournalEntryLineEntity debitLine = new JournalEntryLineEntity(
                senderLedgerAccount,
                amount,
                BigDecimal.ZERO
        );

        // Receiver: CREDIT customer deposit liability
        JournalEntryLineEntity creditLine = new JournalEntryLineEntity(
                receiverLedgerAccount,
                BigDecimal.ZERO,
                amount
        );

        entry.addLine(debitLine);
        entry.addLine(creditLine);

        // Validate double-entry invariants before persisting
        LedgerDomainValidator.validateEntryInvariants(entry);

        return journalEntryRepository.save(entry);
    }
}
