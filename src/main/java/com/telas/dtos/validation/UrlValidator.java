package com.telas.dtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UrlValidator implements ConstraintValidator<ValidUrl, String> {
    private static final String URL_REGEX = "^(?:https?://|www\\.)((?!-)[A-Za-z0-9-]{1,63}(?<!-)(?:\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*)\\.([A-Za-z]{2,63})(?::\\d{1,5})?(?:/\\S*)?(?:\\?[^\\s#]*)?(?:#\\S*)?$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(URL_REGEX);
    }

}
