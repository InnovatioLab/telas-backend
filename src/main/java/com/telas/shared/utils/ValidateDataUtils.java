package com.telas.shared.utils;

import com.telas.shared.constants.SharedConstants;

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
        return Objects.isNull(valor) || valor.trim().isEmpty();
    }

    public static int countCsvIds(String csv) {
        if (isNullOrEmptyString(csv)) {
            return 0;
        }
        int count = 0;
        for (String part : csv.split(",")) {
            if (!part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
