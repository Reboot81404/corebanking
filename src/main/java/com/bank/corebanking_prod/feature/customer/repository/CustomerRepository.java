package com.bank.corebanking_prod.feature.customer.repository;

import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    //search
    Optional<CustomerEntity> findByTckn(String tckn);
    Optional<CustomerEntity> findByEmail(String email);

    // database check if exist
    boolean existsByTckn(String tckn);
    boolean existsByEmail(String email);
}