package com.telas.enums;

import java.util.Map;

public enum PaymentStatus {
  PENDING,
  COMPLETED,
  CANCELLED,
  FAILED;

  private static final Map<String, PaymentStatus> STRIPE_STATUS_MAPPING = Map.of(
          // PaymentIntent statuses
          "succeeded", COMPLETED,
          "requires_payment_method", FAILED,
          "processing", PENDING,
          "canceled", CANCELLED,

          // Subscription statuses
          "active", COMPLETED,
          "incomplete", PENDING,
          "incomplete_expired", FAILED,
          "past_due", PENDING,
          "canceled", CANCELLED,
          "unpaid", FAILED
//          "paused", PAUSED
  );

  public static PaymentStatus fromStripeStatus(String stripeStatus) {
    if (stripeStatus == "paused") {
      return;
    }
    return STRIPE_STATUS_MAPPING.getOrDefault(stripeStatus, FAILED);
  }

}