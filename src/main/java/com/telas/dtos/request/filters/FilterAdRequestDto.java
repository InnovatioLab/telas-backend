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
public class FilterAdRequestDto extends PaginationRequestDto {
    private String genericFilter;

    public Sort setOrdering() {
        return switch (getSortBy()) {
            case "identificationNumber" -> Sort.by(Sort.Order.by("client.identificationNumber"));
            case "clientName" -> Sort.by(Sort.Order.by("client.businessName").ignoreCase());
            case "role" -> Sort.by(Sort.Order.by("client.role").ignoreCase());
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }
}


