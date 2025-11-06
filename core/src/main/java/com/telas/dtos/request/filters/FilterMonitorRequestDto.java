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
public class FilterMonitorRequestDto extends PaginationRequestDto {
    private String genericFilter;

    public Sort setOrdering() {
        return "address".equals(getSortBy())
                ? Sort.by(Sort.Order.by("address.street").ignoreCase())
                : Sort.by(Sort.Order.desc("active"));
    }

}
