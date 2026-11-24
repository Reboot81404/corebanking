package com.bank.corebanking_prod.feature.auth.service;

import com.bank.corebanking_prod.feature.auth.model.dto.LoginRequest;
import com.bank.corebanking_prod.feature.auth.model.dto.TokenResponse;
import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;

public interface AuthenticationService {

    TokenResponse login(LoginRequest request);

    UserIdentityEntity provisionUser(String username, String rawPassword, UserRole role, Long customerId);
}
