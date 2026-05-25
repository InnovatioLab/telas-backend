package com.telas.shared.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static java.util.Locale.US;

public final class SpecificationFactory {

    private SpecificationFactory() {
    }

    public static void addDatePredicates(List<Predicate> predicates, CriteriaBuilder criteriaBuilder,
            Root<?> root, String genericFilter, String... dateFields) {
        LocalDate parsed = parseFilterDate(genericFilter);
        if (parsed == null) {
            return;
        }
        for (String dateField : dateFields) {
            predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.function("date", LocalDate.class, root.get(dateField)),
                    parsed));
        }
    }

    public static void addIdPredicate(List<Predicate> predicates, CriteriaBuilder criteriaBuilder,
            Root<?> root, String idField, String genericFilter) {
        try {
            UUID id = UUID.fromString(genericFilter);
            predicates.add(criteriaBuilder.equal(root.get(idField), id));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static LocalDate parseFilterDate(String genericFilter) {
        try {
            return LocalDate.parse(genericFilter);
        } catch (DateTimeParseException ignored) {
        }
        try {
            DateTimeFormatter usFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", US);
            return LocalDate.parse(genericFilter, usFormatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
