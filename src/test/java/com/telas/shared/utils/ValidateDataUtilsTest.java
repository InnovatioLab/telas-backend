package com.telas.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateDataUtilsTest {

    @Test
    void parseCsvUuids_shouldIgnoreEmptySegments() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        List<UUID> parsed = ValidateDataUtils.parseCsvUuids(first + ",," + second + ",");

        assertEquals(List.of(first, second), parsed);
    }

    @Test
    void parseCsvUuids_shouldIgnoreInvalidValues() {
        UUID valid = UUID.randomUUID();

        List<UUID> parsed = ValidateDataUtils.parseCsvUuids(valid + ",not-a-uuid,,");

        assertEquals(List.of(valid), parsed);
    }

    @Test
    void parseCsvUuids_shouldReturnEmptyListForBlankInput() {
        assertTrue(ValidateDataUtils.parseCsvUuids(null).isEmpty());
        assertTrue(ValidateDataUtils.parseCsvUuids("").isEmpty());
        assertTrue(ValidateDataUtils.parseCsvUuids("   ").isEmpty());
        assertTrue(ValidateDataUtils.parseCsvUuids(",,,").isEmpty());
    }
}
