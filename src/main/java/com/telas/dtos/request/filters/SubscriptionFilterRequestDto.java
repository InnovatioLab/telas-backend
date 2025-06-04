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
public class SubscriptionFilterRequestDto extends PaginationRequestDto {
  private String genericFilter;

  public Sort setOrdering() {
    return switch (getSortBy()) {
      case "amount" -> Sort.by(Sort.Order.by("amount"));
      case "recurrence" -> Sort.by(Sort.Order.by("recurrence").ignoreCase());
      case "status" -> Sort.by(Sort.Order.by("status").ignoreCase());
      case "startedAt" -> Sort.by(Sort.Order.by("startedAt"));
      case "endsAt" -> Sort.by(Sort.Order.by("endsAt"));
      default -> Sort.by(Sort.Order.desc("startedAt"));
    };
  }
}


