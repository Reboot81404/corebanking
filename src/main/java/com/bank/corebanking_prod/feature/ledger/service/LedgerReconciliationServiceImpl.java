package com.bank.corebanking_prod.feature.ledger.service;

import com.bank.corebanking_prod.feature.account.model.dto.AccountDto;
import com.bank.corebanking_prod.feature.account.service.AccountService;
import com.bank.corebanking_prod.feature.ledger.domain.LedgerDomainValidator;
import com.bank.corebanking_prod.feature.ledger.model.dto.ReconciliationReportDto;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.repository.JournalEntryLineRepository;
import com.bank.corebanking_prod.feature.ledger.repository.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LedgerReconciliationServiceImpl implements LedgerReconciliationService {

    private final AccountService accountService;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public LedgerReconciliationServiceImpl(
            AccountService accountService,
            LedgerAccountRepository ledgerAccountRepository,
            JournalEntryLineRepository journalEntryLineRepository) {
        this.accountService = accountService;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationReportDto reconcileCustomerDeposits() {
        List<AccountDto.Response> accounts = accountService.getAllAccounts();

        int matchedAccounts = 0;
        int discrepancyCount = 0;
        List<ReconciliationReportDto.DiscrepancyItem> discrepancies = new ArrayList<>();
        List<Long> missingLedgerAccounts = new ArrayList<>();

        for (AccountDto.Response account : accounts) {
            Optional<LedgerAccountEntity> optLedger = ledgerAccountRepository.findByBankAccountId(account.getId());
            if (optLedger.isEmpty()) {
                missingLedgerAccounts.add(account.getId());
                continue;
            }

            LedgerAccountEntity la = optLedger.get();
            BigDecimal totalDebit = journalEntryLineRepository.sumDebitByLedgerAccountId(la.getId());
            BigDecimal totalCredit = journalEntryLineRepository.sumCreditByLedgerAccountId(la.getId());

            BigDecimal ledgerCalculatedBalance = LedgerDomainValidator.calculateAccountBalance(
                    la.getAccountType(),
                    la.getNormalBalance(),
                    totalDebit,
                    totalCredit
            );

            BigDecimal accountBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;

            if (ledgerCalculatedBalance.compareTo(accountBalance) == 0) {
                matchedAccounts++;
            } else {
                discrepancyCount++;
                discrepancies.add(new ReconciliationReportDto.DiscrepancyItem(
                        account.getId(),
                        account.getIban(),
                        accountBalance,
                        ledgerCalculatedBalance,
                        accountBalance.subtract(ledgerCalculatedBalance)
                ));
            }
        }

        return new ReconciliationReportDto(
                accounts.size(),
                matchedAccounts,
                discrepancyCount,
                discrepancies,
                missingLedgerAccounts
        );
    }
}
