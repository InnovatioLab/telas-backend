package com.marketingproject.shared.utils;

import com.marketingproject.shared.constants.SharedConstants;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ValidateDataUtils {

    private ValidateDataUtils() {
    }

    public static boolean containsOnlyNumbers(String text) {
        return Pattern.matches(SharedConstants.REGEX_ONLY_NUMBERS, text);
    }

    public static boolean isNullOrEmpty(List<?> lista) {
        return Objects.isNull(lista) || lista.isEmpty();
    }

    public static boolean isNullOrEmptyString(String valor) {
        return Objects.isNull(valor) || valor.isEmpty();
    }
}
