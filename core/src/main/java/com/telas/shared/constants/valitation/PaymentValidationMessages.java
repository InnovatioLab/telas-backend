package com.telas.shared.constants.valitation;

public final class PaymentValidationMessages {
  public static final String PAYMENT_NOT_FOUND = "Payment not found";
  public static final String ID_CANNOT_BE_EMPTY = "ID cannot be empty";
  public static final String STATUS_CANNOT_BE_EMPTY = "Status cannot be empty";
  public static final String PAYMENT_PROCESSING_ERROR = "Error processing subscription payment, please try again later";
  public static final String PAYMENT_CLIENT_MISMATCH = "Payment client does not match with paymentId: ";
  public static final String PAYMENT_INTENT_PROCESSING_ERROR = "Error processing PaymentIntent with paymentIntentId: ";
  public static final String PAYMENT_PRODUCT_PRICES_NOT_FOUND = "Product prices not found for product";
  public static final String PAYMENT_UPDATE_DEFAULT_SOURCE_ERROR = "Error updating default source for payment method with id: ";
}
