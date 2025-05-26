package com.telas.services.impl;

import com.telas.entities.Cart;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.helpers.SubscriptionHelper;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.PaymentService;
import com.telas.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
  private final SubscriptionRepository repository;
  private final AuthenticatedUserService authenticatedUserService;
  private final PaymentService paymentService;
  private final SubscriptionHelper helper;

  @Override
  public String save() {
    Client client = authenticatedUserService.getLoggedUser().client();
    Cart cart = helper.getActiveCart(client);
    Subscription subscription = new Subscription(client, cart);

    if (subscription.isBonus()) {
      subscription.initialize();
      repository.save(subscription);
      return null;
    }

    subscription.setAmount(helper.calculateTotalPrice(cart.getItems()));
    repository.save(subscription);
    return paymentService.process(subscription);
  }

  @Override
  public Subscription findById(UUID subscriptionId) {
    return null;
  }

  @Override
  @Transactional
  public void cancelSubscription(com.stripe.model.Subscription subscription) {

  }
}
