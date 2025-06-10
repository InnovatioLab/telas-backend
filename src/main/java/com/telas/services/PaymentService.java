package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;

public interface PaymentService {
  String process(Subscription subscription, Recurrence recurrence);

  void updatePaymentStatus(PaymentIntent paymentIntent) throws StripeException, JsonProcessingException;

  void updatePaymentStatus(Invoice invoice) throws JsonProcessingException, StripeException;
}
