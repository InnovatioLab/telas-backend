package com.telas.shared.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecificationFactoryTest {

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Root<Object> root;

    @Mock
    private Path<Object> startedAtPath;

    @Mock
    private Path<Object> endsAtPath;

    @Mock
    private Path<Object> idPath;

    @Mock
    private Expression<LocalDate> dateExpression;

    @Mock
    private Predicate predicate;

    @Test
    void addDatePredicates_addsPredicateForEachFieldWhenIsoDateMatches() {
        List<Predicate> predicates = new ArrayList<>();
        when(root.get("startedAt")).thenReturn(startedAtPath);
        when(root.get("endsAt")).thenReturn(endsAtPath);
        when(criteriaBuilder.function(eq("date"), eq(LocalDate.class), eq(startedAtPath)))
                .thenReturn(dateExpression);
        when(criteriaBuilder.function(eq("date"), eq(LocalDate.class), eq(endsAtPath)))
                .thenReturn(dateExpression);
        when(criteriaBuilder.equal(any(), eq(LocalDate.parse("2024-05-23")))).thenReturn(predicate);

        SpecificationFactory.addDatePredicates(predicates, criteriaBuilder, root, "2024-05-23", "startedAt", "endsAt");

        assertEquals(2, predicates.size());
    }

    @Test
    void addIdPredicate_addsPredicateWhenUuidMatches() {
        List<Predicate> predicates = new ArrayList<>();
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(root.get("id")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, id)).thenReturn(predicate);

        SpecificationFactory.addIdPredicate(
                predicates,
                criteriaBuilder,
                root,
                "id",
                "550e8400-e29b-41d4-a716-446655440000");

        verify(criteriaBuilder).equal(idPath, id);
    }
}
