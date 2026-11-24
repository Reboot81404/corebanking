package com.bank.corebanking_prod.feature.ledger.repository;

import com.bank.corebanking_prod.feature.ledger.model.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Long> {

    Optional<JournalEntryEntity> findByReferenceNumber(String referenceNumber);

    Optional<JournalEntryEntity> findByTransactionHistoryId(Long transactionHistoryId);

    boolean existsByTransactionHistoryId(Long transactionHistoryId);
}
