package com.bank.corebanking_prod.feature.account.repository;

import com.bank.corebanking_prod.feature.account.model.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByIban(String iban);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.iban = :iban")
    Optional<AccountEntity> findByIbanForUpdate(@Param("iban") String iban);

    Page<AccountEntity> findByCustomerId(Long customerId, Pageable pageable);
}
