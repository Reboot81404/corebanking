package com.bank.corebanking_prod.feature.auth.model.enums;

public enum UserRole {
    CUSTOMER,
    BANK_OPERATOR,
    AUDITOR,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
