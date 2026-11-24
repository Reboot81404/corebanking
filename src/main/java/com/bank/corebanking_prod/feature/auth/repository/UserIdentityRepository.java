package com.bank.corebanking_prod.feature.auth.repository;

import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {

    Optional<UserIdentityEntity> findByUsername(String username);

    Optional<UserIdentityEntity> findByCustomerId(Long customerId);

    boolean existsByUsername(String username);

    boolean existsByCustomerId(Long customerId);

    boolean existsByRole(UserRole role);
}
