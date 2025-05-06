package com.marketingproject.dtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class EINValidator implements ConstraintValidator<ValidEIN, String> {
    private static final Pattern EIN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Set<String> VALID_PREFIXES = Set.of(
            "01", "02", "03", "04", "05", "06", "10", "11", "12", "13", "14", "15", "16", "20", "21", "22", "23", "24", "25", "26", "27", "30", "31", "32",
            "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "50", "51", "52", "53", "54", "55", "56", "57",
            "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "71", "72", "73", "74", "75", "76", "77", "80", "81", "82", "83", "84", "85",
            "86", "87", "88", "90", "91", "92", "93", "94", "95", "98", "99"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !EIN_PATTERN.matcher(value).matches()) {
            return false;
        }

        String prefix = value.substring(0, 2);
        return VALID_PREFIXES.contains(prefix);
    }
}
