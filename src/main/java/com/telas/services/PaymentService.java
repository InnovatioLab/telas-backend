package com.telas.services;

import com.stripe.model.PaymentIntent;
import com.telas.entities.Subscription;

public interface PaymentService {
  String process(Subscription subscription);

  void updatePaymentStatus(PaymentIntent paymentIntent);

  void updatePaymentStatus(com.stripe.model.Subscription subscription);
}
