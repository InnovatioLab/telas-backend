package com.telas.enums;

import com.telas.entities.Subscription;

import java.time.Instant;

public enum SubscriptionStatus {
  PENDING,
  ACTIVE,
  EXPIRED,
  CANCELLED;

  public static SubscriptionStatus fromStripeStatus(String stripePaymentStatus, String invoiceStatus, Subscription subscription) {
    if (stripePaymentStatus != null) {
      return switch (stripePaymentStatus) {
        case "succeeded" -> ACTIVE;
        case "canceled" -> CANCELLED;
        case "requires_payment_method", "unpaid", "incomplete_expired" -> EXPIRED;
        default -> subscription.getStatus();
      };
    }

    if (invoiceStatus != null) {
      return switch (invoiceStatus) {
        case "paid" -> ACTIVE;
        case "unpaid", "void" -> EXPIRED;
        case "open" ->
                subscription.getEndsAt() != null && subscription.getEndsAt().isBefore(Instant.now()) ? EXPIRED : subscription.getStatus();
        default -> subscription.getStatus();
      };
    }

    return subscription.getStatus();
  }
}
