package com.telas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.SubscriptionMinResponseDto;
import com.telas.entities.Subscription;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
  String save();

  Subscription findById(UUID subscriptionId);

  void cancelSubscription(com.stripe.model.Subscription stripeSubscription) throws JsonProcessingException;

  void cancelSubscription(UUID subscriptionId);

  void removeAdsFromExpiredSubscriptions();

  PaginationResponseDto<List<SubscriptionMinResponseDto>> findClientSubscriptionsFilters(SubscriptionFilterRequestDto request);
}
