package com.telas.dtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SSNValidator implements ConstraintValidator<ValidSSN, String> {
    private static final Pattern SSN_PATTERN = Pattern.compile("^(?!(000|666|9\\d{2}))(?:\\d{3}-\\d{2}-\\d{4}|\\d{9})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && SSN_PATTERN.matcher(value).matches();
    }
}

