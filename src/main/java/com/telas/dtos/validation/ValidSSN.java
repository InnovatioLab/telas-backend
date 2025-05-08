package com.telas.dtos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SSNValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSSN {
    String message() default "Invalid Identification Number (SSN)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
