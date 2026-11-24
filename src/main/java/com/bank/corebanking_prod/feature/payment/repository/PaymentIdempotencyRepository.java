package com.bank.corebanking_prod.feature.payment.repository;

import com.bank.corebanking_prod.feature.payment.model.entity.PaymentIdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotencyEntity, Long> {

    Optional<PaymentIdempotencyEntity> findByPrincipalIdAndOperationAndIdempotencyKey(
            Long principalId, String operation, String idempotencyKey);
}
