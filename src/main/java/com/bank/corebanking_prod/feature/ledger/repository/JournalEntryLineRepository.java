package com.bank.corebanking_prod.feature.ledger.repository;

import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLineEntity, Long> {

    @Query("SELECT COALESCE(SUM(l.debitAmount), 0.00) FROM JournalEntryLineEntity l WHERE l.ledgerAccount.id = :accountId")
    BigDecimal sumDebitByLedgerAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(l.creditAmount), 0.00) FROM JournalEntryLineEntity l WHERE l.ledgerAccount.id = :accountId")
    BigDecimal sumCreditByLedgerAccountId(@Param("accountId") Long accountId);

    List<JournalEntryLineEntity> findByJournalEntryId(Long journalEntryId);
}
