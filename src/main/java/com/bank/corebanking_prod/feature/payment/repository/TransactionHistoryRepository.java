package com.bank.corebanking_prod.feature.payment.repository;

import com.bank.corebanking_prod.feature.payment.model.entity.TransactionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistoryEntity, Long> {

    // Read/select with using sender or receiver
    List<TransactionHistoryEntity> findBySenderIbanOrReceiverIban(String senderIban, String receiverIban);
}