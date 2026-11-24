package com.bank.corebanking_prod.feature.account.model.dto;

import com.bank.corebanking_prod.feature.account.model.enums.Currency;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccountDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Balance null olduğunda validasyon başarılı olmalı (omitted balance)")
    void shouldPassValidationWhenBalanceIsNull() {
        AccountDto.Create dto = new AccountDto.Create(1L, Currency.TRY, null);
        Set<ConstraintViolation<AccountDto.Create>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Null balance should be valid");
    }

    @Test
    @DisplayName("Balance 0 veya 0.00 olduğunda validasyon başarılı olmalı")
    void shouldPassValidationWhenBalanceIsZero() {
        AccountDto.Create dto1 = new AccountDto.Create(1L, Currency.TRY, BigDecimal.ZERO);
        Set<ConstraintViolation<AccountDto.Create>> violations1 = validator.validate(dto1);
        assertTrue(violations1.isEmpty(), "Zero balance should be valid");

        AccountDto.Create dto2 = new AccountDto.Create(1L, Currency.TRY, new BigDecimal("0.00"));
        Set<ConstraintViolation<AccountDto.Create>> violations2 = validator.validate(dto2);
        assertTrue(violations2.isEmpty(), "0.00 balance should be valid");
    }

    @Test
    @DisplayName("Balance pozitif olduğunda validasyon başarısız olmalı")
    void shouldFailValidationWhenBalanceIsPositive() {
        AccountDto.Create dto = new AccountDto.Create(1L, Currency.TRY, new BigDecimal("100.00"));
        Set<ConstraintViolation<AccountDto.Create>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Positive balance should be invalid");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    @Test
    @DisplayName("Balance negatif olduğunda validasyon başarısız olmalı")
    void shouldFailValidationWhenBalanceIsNegative() {
        AccountDto.Create dto = new AccountDto.Create(1L, Currency.TRY, new BigDecimal("-10.00"));
        Set<ConstraintViolation<AccountDto.Create>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Negative balance should be invalid");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }
}
