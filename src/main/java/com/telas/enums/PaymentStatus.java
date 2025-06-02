package com.telas.enums;

import com.telas.entities.Payment;

import java.util.Map;

public enum PaymentStatus {
  PENDING,
  COMPLETED,
  CANCELLED,
  FAILED;

  private static final Map<String, PaymentStatus> STRIPE_PAYMENT_INTENT_STATUS_MAPPING = Map.of(
          "succeeded", COMPLETED,
          "processing", PENDING,
          "requires_payment_method", FAILED,
          "canceled", CANCELLED
  );

  private static final Map<String, PaymentStatus> STRIPE_INVOICE_STATUS_MAPPING = Map.of(
          "paid", COMPLETED,
          "open", FAILED,
          "unpaid", FAILED,
          "void", FAILED
  );

  public static PaymentStatus fromStripeStatus(String paymentIntentStatus, String invoiceStatus, Payment payment) {
    if (paymentIntentStatus != null) {
      return STRIPE_PAYMENT_INTENT_STATUS_MAPPING.getOrDefault(paymentIntentStatus, payment.getStatus());
    }

    if (invoiceStatus != null) {
      return STRIPE_INVOICE_STATUS_MAPPING.getOrDefault(invoiceStatus, payment.getStatus());
    }

    return payment.getStatus();
  }
}