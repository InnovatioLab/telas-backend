package com.marketingproject.dtos.request.filters;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilterPendingAttachmentRequestDto extends PaginationRequestDto {
    private String genericFilter;

    public Sort setOrdering() {
        return !Objects.equals(getSortBy(), "name")
                ? Sort.by(Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.by("name").ignoreCase());
    }
}


