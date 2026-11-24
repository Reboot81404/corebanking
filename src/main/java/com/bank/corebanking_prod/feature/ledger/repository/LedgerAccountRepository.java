package com.bank.corebanking_prod.feature.ledger.repository;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import com.bank.corebanking_prod.feature.ledger.model.entity.LedgerAccountEntity;
import com.bank.corebanking_prod.feature.ledger.model.enums.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, Long> {

    Optional<LedgerAccountEntity> findByAccountCode(String accountCode);

    Optional<LedgerAccountEntity> findByBankAccountId(Long bankAccountId);

    Optional<LedgerAccountEntity> findByAccountTypeAndCurrency(LedgerAccountType accountType, Currency currency);

    List<LedgerAccountEntity> findByAccountType(LedgerAccountType accountType);
}
