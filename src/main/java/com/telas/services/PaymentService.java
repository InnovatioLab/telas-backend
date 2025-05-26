package com.telas.services;

import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.telas.entities.Subscription;

public interface PaymentService {
  String process(Subscription subscription);

  void updatePaymentStatus(PaymentIntent paymentIntent);

  void updatePaymentStatus(Invoice invoice);
}
