package com.bank.corebanking_prod.common.validation.tckn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TcknValidatorTest {

    private final TcknValidator validator = new TcknValidator();

    @Test
    void shouldAcceptMathematicallyValidTckn() {
        assertTrue(validator.isValid("10000000146", null));
    }

    @Test
    void shouldRejectTcknStartingWithZero() {
        assertFalse(validator.isValid("01000000146", null));
    }

    @Test
    void shouldRejectTcknWithWrongLength() {
        assertFalse(validator.isValid("1000000146", null));
    }

    @Test
    void shouldRejectTcknContainingNonNumericCharacters() {
        assertFalse(validator.isValid("10000000A46", null));
    }

    @Test
    void shouldRejectNullTckn() {
        assertFalse(validator.isValid(null, null));
    }
}
