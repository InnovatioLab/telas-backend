package com.telas.services;

import com.telas.dtos.response.PaymentInfoResponseDto;
import com.telas.entities.Subscription;

import java.util.UUID;

public interface SubscriptionService {
  PaymentInfoResponseDto save();

  Subscription findById(UUID subscriptionId);

}
