package com.telas.services;

import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;

public interface PaymentService {
  String process(Subscription subscription, Recurrence recurrence);

  void updatePaymentStatus(PaymentIntent paymentIntent);

  void updatePaymentStatus(Invoice invoice);

  void handleDisputeFundsWithdrawn(com.stripe.model.Dispute dispute);
}
