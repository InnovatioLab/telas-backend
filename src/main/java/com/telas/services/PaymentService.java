package com.telas.services;

import com.stripe.exception.StripeException;
import com.telas.entities.Subscription;

public interface PaymentService {
    String process(Subscription subscription) throws StripeException;
}
