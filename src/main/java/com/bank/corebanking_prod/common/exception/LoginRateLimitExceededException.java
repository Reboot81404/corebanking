package com.bank.corebanking_prod.common.exception;

public class LoginRateLimitExceededException extends RuntimeException {

    public LoginRateLimitExceededException(String message) {
        super(message);
    }
}
