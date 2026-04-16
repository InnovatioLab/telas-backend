package com.telas.dtos.request.filters;

import com.telas.enums.AdValidationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
public class AdminAdOperationsFilterRequestDto extends PaginationRequestDto {
    private String genericFilter;
    private AdValidationType validation;

    public Sort setOrdering() {
        return switch (getSortBy() != null ? getSortBy() : "adName") {
            case "partnerName" -> Sort.by(Sort.Order.by("partnerBusinessName").ignoreCase());
            case "screenAddress" -> Sort.by(Sort.Order.by("screenAddressSummary").ignoreCase());
            case "expiresAt" -> Sort.by(Sort.Order.by("subscriptionEndsAt"));
            case "advertiserName" -> Sort.by(Sort.Order.by("advertiserBusinessName").ignoreCase());
            default -> Sort.by(Sort.Order.by("adName").ignoreCase());
        };
    }
}
