package com.telas.services.impl;

import com.telas.entities.Cart;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.enums.SubscriptionStatus;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
  private final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
  private final SubscriptionRepository repository;
  private final ClientRepository clientRepository;
  private final AuthenticatedUserService authenticatedUserService;
  private final PaymentService paymentService;
  private final SubscriptionHelper helper;

  @Override
  @Transactional
  public String save() {
    Client client = authenticatedUserService.getLoggedUser().client();
    Cart cart = helper.getActiveCart(client);
    Subscription subscription = new Subscription(client, cart);

    if (subscription.isBonus()) {
      subscription.initialize();
      persistSubscriptionClient(client, subscription);
      return null;
    }

    persistSubscriptionClient(client, subscription);
    return paymentService.process(subscription);
  }

  @Override
  public Subscription findById(UUID subscriptionId) {
    return null;
  }

  @Override
  @Transactional
  public void cancelSubscription(com.stripe.model.Subscription stripeSubscription) {
    UUID subscriptionId = UUID.fromString(stripeSubscription.getMetadata().get("subscriptionId"));
    Subscription subscription = helper.findEntityById(subscriptionId);
    List<SubscriptionStatus> invalidStatuses = List.of(SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED);

    if (invalidStatuses.contains(subscription.getStatus())) {
      log.info("Subscription with id: {} is already cancelled or expired.", subscriptionId);
      return;
    }

    log.info("Handling subscription deletion for id: {}", subscriptionId);
    subscription.setStatus(SubscriptionStatus.CANCELLED);
    subscription.setEndsAt(Instant.ofEpochSecond(stripeSubscription.getCanceledAt()));
    repository.save(subscription);

    // Remove monitors and ads associated with the subscription
    helper.removeMonitorAdsFromSubscription(subscription);
  }

  private void persistSubscriptionClient(Client client, Subscription subscription) {
    repository.save(subscription);
    client.getSubscriptions().add(subscription);
    clientRepository.save(client);
  }
}
