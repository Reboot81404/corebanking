
package com.bank.corebanking_prod.common.validation.tckn;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TcknValidator
        implements ConstraintValidator<ValidTckn, String> {

    @Override
    public boolean isValid(
            String tckn,
            ConstraintValidatorContext context) {

        if (tckn == null || !tckn.matches("[0-9]{11}")) {
            return false;
        }

        if (tckn.charAt(0) == '0') {
            return false;
        }

        int[] digits = new int[11];

        for (int i = 0; i < 11; i++) {
            digits[i] = Character.getNumericValue(tckn.charAt(i));
        }

        int oddSum =
                digits[0] + digits[2] + digits[4]
                        + digits[6] + digits[8];

        int evenSum =
                digits[1] + digits[3]
                        + digits[5] + digits[7];

        int tenthDigit = ((oddSum * 7) - evenSum) % 10;

        if (tenthDigit < 0) {
            tenthDigit += 10;
        }

        if (digits[9] != tenthDigit) {
            return false;
        }

        int firstTenSum = 0;

        for (int i = 0; i < 10; i++) {
            firstTenSum += digits[i];
        }

        int eleventhDigit = firstTenSum % 10;

        return digits[10] == eleventhDigit;
    }
}
