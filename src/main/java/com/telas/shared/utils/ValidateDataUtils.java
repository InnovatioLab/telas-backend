package com.telas.shared.utils;

import com.telas.shared.constants.SharedConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
        return parseCsvUuids(csv).size();
    }

    public static List<UUID> parseCsvUuids(String csv) {
        if (isNullOrEmptyString(csv)) {
            return List.of();
        }
        List<UUID> parsed = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                parsed.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return List.copyOf(parsed);
    }
}
