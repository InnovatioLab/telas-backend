package com.telas.enums;

public enum SubscriptionStatus {
  PENDING,
  ACTIVE,
  EXPIRED,
  CANCELLED;

  public static SubscriptionStatus fromStripeStatus(String stripePaymentStatus, String stripeSubscriptionStatus) {
    if (stripeSubscriptionStatus != null) {
      return switch (stripeSubscriptionStatus) {
        case "active" -> ACTIVE;
        case "canceled" -> CANCELLED;
        case "past_due", "unpaid", "incomplete_expired" -> EXPIRED;
        default -> PENDING;
      };
    }

    if (stripePaymentStatus != null) {
      return switch (stripePaymentStatus) {
        case "succeeded" -> ACTIVE;
        case "canceled" -> CANCELLED;
        case "requires_payment_method", "unpaid", "incomplete_expired" -> EXPIRED;
        default -> PENDING;
      };
    }

    return PENDING;
  }
}
