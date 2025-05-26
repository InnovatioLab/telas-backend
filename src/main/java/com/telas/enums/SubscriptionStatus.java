package com.telas.enums;

import java.util.Map;

public enum SubscriptionStatus {
  PENDING,
  ACTIVE,
  EXPIRED,
  CANCELLED,
  FAILED,
  PAUSED;


  private static final Map<String, SubscriptionStatus> STRIPE_STATUS_MAPPING = Map.of(
          "past_due", PENDING,
          "incomplete", PENDING,
          "active", ACTIVE,
          "incomplete_expired", EXPIRED,
          "canceled", CANCELLED,
          "unpaid", FAILED,
          "paused", PAUSED
  );

  public static SubscriptionStatus fromStripeStatus(String stripeStatus) {
    return STRIPE_STATUS_MAPPING.getOrDefault(stripeStatus, FAILED);
  }

}
