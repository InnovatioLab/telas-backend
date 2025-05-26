package com.telas.enums;

import com.telas.entities.Payment;

import java.util.Map;

public enum PaymentStatus {
  PENDING,
  COMPLETED,
  CANCELLED,
  FAILED;

  private static final Map<String, PaymentStatus> STRIPE_STATUS_MAPPING = Map.of(
          "succeeded", COMPLETED,
          "active", COMPLETED,
          "processing", PENDING,
          "incomplete", PENDING,
          "canceled", CANCELLED,
          "incomplete_expired", FAILED,
          "unpaid", FAILED,
          "requires_payment_method", FAILED
  );

  public static PaymentStatus fromStripeStatus(String stripePaymentStatus, String stripeSubscriptionStatus, Payment payment) {
    if (stripeSubscriptionStatus != null) {
      return STRIPE_STATUS_MAPPING.getOrDefault(stripeSubscriptionStatus, payment.getStatus());
    }

    if (stripePaymentStatus != null) {
      return STRIPE_STATUS_MAPPING.getOrDefault(stripePaymentStatus, payment.getStatus());
    }

    return payment.getStatus();
  }
}