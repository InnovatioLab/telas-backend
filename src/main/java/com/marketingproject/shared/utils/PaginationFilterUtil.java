package com.marketingproject.shared.utils;

import com.marketingproject.dtos.request.PaginationRequestDto;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.shared.constants.MessageCommonsConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;

import java.util.function.BiFunction;

public class PaginationFilterUtil {
    private PaginationFilterUtil() {
    }

    public static Pageable getPageable(PaginationRequestDto pageRequest, Sort order) {
        if (pageRequest.getPage() <= 0) {
            throw new BusinessRuleException(MessageCommonsConstants.START_PAGE_ERROR_MESSAGE);
        }

        if (pageRequest.getSortDir() != null) {
            order = pageRequest.getSortDir().equalsIgnoreCase("asc") ? order.ascending() : order.descending();
        }

        return PageRequest.of(
                pageRequest.getPage() - 1,
                pageRequest.getSize(),
                order
        );
    }

    public static <T, U> Specification<T> addSpecificationFilter(Specification<T> initialSpec, U valor,
                                                                 BiFunction<Specification<T>, U, Specification<T>> filter) {
        Specification<T> specification = initialSpec != null ? Specification.where(initialSpec) : Specification.where(null);
        return addFilters(specification, valor, filter);
    }

    protected static <T, U> Specification<T> addFilters(Specification<T> specification, U valor,
                                                        BiFunction<Specification<T>, U, Specification<T>> filter) {
        return !ObjectUtils.isEmpty(valor) ? filter.apply(specification, valor) : specification;
    }
}
