package com.telas.dtos.request.filters;

import com.telas.enums.AdValidationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class AdminAdOperationsFilterRequestDto extends PaginationRequestDto {
    private String genericFilter;
    private AdValidationType validation;

    private String advertiserName;
    private String partnerName;
    private String boxIp;
    private String screenContains;
    private Instant submissionDateFrom;
    private Instant submissionDateTo;

    public Sort resolveSort(AdValidationType validation) {
        String key = effectiveSortBy(validation);
        AdValidationType branch = validation != null ? validation : AdValidationType.APPROVED;
        Sort base = switch (branch) {
            case APPROVED -> sortForApprovedPlacement(key);
            case PENDING, REJECTED -> sortForPendingWithoutPlacement(key);
            default -> sortForMonitorAd(key);
        };
        return base;
    }

    private String effectiveSortBy(AdValidationType validation) {
        String s = getSortBy();
        AdValidationType forDefaultKey = validation != null ? validation : AdValidationType.APPROVED;
        if (s == null || s.isBlank() || "firstName".equals(s)) {
            return defaultSortKey(forDefaultKey);
        }
        return s;
    }

    private static String defaultSortKey(AdValidationType validation) {
        if (validation == AdValidationType.APPROVED) {
            return "submissionDate";
        }
        return "adName";
    }

    private static Sort sortForApprovedPlacement(String key) {
        return switch (key) {
            case "partnerName" -> Sort.by(Sort.Order.by("partner.businessName").ignoreCase());
            case "screenAddress" -> Sort.by(Sort.Order.by("addr.street").ignoreCase());
            case "expiresAt" -> Sort.by("sub.endsAt");
            case "advertiserName" -> Sort.by(Sort.Order.by("advertiser.businessName").ignoreCase());
            case "submissionDate", "adCreatedAt" -> Sort.by("ad.createdAt");
            default -> Sort.by(Sort.Order.by("ad.name").ignoreCase());
        };
    }

    private static Sort sortForPendingWithoutPlacement(String key) {
        return switch (key) {
            case "advertiserName" -> Sort.by(Sort.Order.by("client.businessName").ignoreCase());
            case "submissionDate", "adCreatedAt" -> Sort.by("ad.createdAt");
            default -> Sort.by(Sort.Order.by("ad.name").ignoreCase());
        };
    }

    private static Sort sortForMonitorAd(String key) {
        return switch (key) {
            case "partnerName" -> Sort.by(Sort.Order.by("partner.businessName").ignoreCase());
            case "screenAddress" -> Sort.by(Sort.Order.by("addr.street").ignoreCase());
            case "expiresAt" -> Sort.by("sub.endsAt");
            case "advertiserName" -> Sort.by(Sort.Order.by("advertiser.businessName").ignoreCase());
            case "submissionDate", "adCreatedAt" -> Sort.by("ad.createdAt");
            default -> Sort.by(Sort.Order.by("ad.name").ignoreCase());
        };
    }

    public Sort setOrdering() {
        return resolveSort(validation);
    }
}
