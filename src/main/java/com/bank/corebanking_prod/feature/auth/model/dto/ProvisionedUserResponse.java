package com.bank.corebanking_prod.feature.auth.model.dto;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import com.bank.corebanking_prod.feature.auth.model.enums.UserStatus;

public record ProvisionedUserResponse(
        Long id,
        String username,
        UserRole role,
        UserStatus status,
        Long customerId
) {
}
