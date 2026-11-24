package com.bank.corebanking_prod.feature.auth.model.dto;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserRole role
) {
    public TokenResponse(String accessToken, long expiresIn, UserRole role) {
        this(accessToken, "Bearer", expiresIn, role);
    }
}
