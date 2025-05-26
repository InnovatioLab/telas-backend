package com.telas.services;

import com.telas.entities.Subscription;

import java.util.UUID;

public interface SubscriptionService {
  String save();

  Subscription findById(UUID subscriptionId);

  void cancelSubscription(com.stripe.model.Subscription subscription);
}
