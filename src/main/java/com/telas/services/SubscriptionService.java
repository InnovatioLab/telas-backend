package com.telas.services;

import com.telas.dtos.request.filters.SubscriptionFilterRequestDto;
import com.telas.dtos.response.PaginationResponseDto;
import com.telas.dtos.response.SubscriptionMinResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.enums.Recurrence;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
  String save();

  SubscriptionResponseDto findById(UUID subscriptionId);

  String upgradeSubscription(UUID subscriptionId, Recurrence recurrence);

  void cancelSubscription(com.stripe.model.Subscription stripeSubscription);

  void cancelSubscription(UUID subscriptionId);

  void removeAdsFromExpiredSubscriptions();

  PaginationResponseDto<List<SubscriptionMinResponseDto>> findClientSubscriptionsFilters(SubscriptionFilterRequestDto request);
}
