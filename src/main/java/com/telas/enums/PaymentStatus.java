package com.telas.enums;

import java.util.Map;

public enum PaymentStatus {
  PENDING,
  COMPLETED,
  CANCELLED,
  FAILED;

  private static final Map<String, PaymentStatus> STRIPE_STATUS_MAPPING = Map.of(
          "succeeded", COMPLETED,
          "failed", FAILED,
          "pending", PENDING,
          "canceled", CANCELLED
  );

  public static PaymentStatus fromStripeStatus(String stripeStatus) {
    return STRIPE_STATUS_MAPPING.getOrDefault(stripeStatus, FAILED);
  }
}