
package com.bank.corebanking_prod.common.validation.tckn;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TcknValidator.class)
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTckn {

    String message() default "Invalid Turkish identification number.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
