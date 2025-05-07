package com.telas.dtos.request.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientFilterRequestDto extends PaginationRequestDto {
    private String genericFilter;

    public Sort setOrdering() {
        return switch (getSortBy()) {
            case "identificationNumber" -> Sort.by(Sort.Order.by("identificationNumber"));
            case "role" -> Sort.by(Sort.Order.by("role").ignoreCase());
            case "businessField" -> Sort.by(Sort.Order.by("businessField").ignoreCase());
            case "status" -> Sort.by(Sort.Order.by("status").ignoreCase());
            case "createdAt" -> Sort.by(Sort.Order.by("createdAt"));
            case "updatedAt" -> Sort.by(Sort.Order.by("updatedAt"));
            default -> Sort.by(Sort.Order.by("businessName").ignoreCase());
        };
    }
}


