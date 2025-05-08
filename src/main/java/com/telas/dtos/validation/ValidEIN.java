package com.telas.dtos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EINValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEIN {
    String message() default "Invalid Identification Number (EIN)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
