package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
  private final SubscriptionRepository repository;
  private final SubscriptionFlowRepository subscriptionFlowRepository;
  private final CartService cartService;
  private final MonitorService monitorService;

  @Transactional
  public Cart getActiveCart(Client client) {
    Cart cart = cartService.findActiveByClientIdWithItens(client.getId());

    validateCart(cart);
    validateItems(cart.getItems());
    return cart;
  }

  @Transactional
  public void inactivateCart(Client client) {
    Cart cart = cartService.findActiveByClientIdWithItens(client.getId());
    cartService.inactivateCart(cart);
  }

  private void validateCart(Cart cart) {
    if (cart.getItems().isEmpty()) {
      throw new BusinessRuleException(CartValidationMessages.CART_EMPTY);
    }

    if (!cart.isActive()) {
      throw new BusinessRuleException(CartValidationMessages.CART_INACTIVE);
    }
  }

  private void validateItems(List<CartItem> items) {
    List<UUID> monitorIds = items.stream()
            .map(item -> item.getMonitor().getId())
            .toList();

    Client client = items.get(0).getCart().getClient();

    List<MonitorValidationResponseDto> results = monitorService.findInvalidMonitorsDuringCheckout(monitorIds, client.getId());

    boolean hasInvalidMonitor = results.stream().anyMatch(result -> !result.isValidMonitor());

    if (hasInvalidMonitor) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE);
    }

    boolean hasActiveSubscriptionWithMonitor = results.stream().anyMatch(MonitorValidationResponseDto::isHasActiveSubscription);

    if (hasActiveSubscriptionWithMonitor) {
      throw new BusinessRuleException(SubscriptionValidationMessages.CLIENT_ALREADY_HAS_ACTIVE_SUBSCRIPTION_WITH_MONITOR);
    }
  }

  @Transactional
  public void removeMonitorAdsFromSubscription(Subscription subscription) {
    monitorService.removeMonitorAdsFromSubscription(subscription);
  }

  @Transactional
  public Subscription findEntityById(UUID subscriptionId) {
    return repository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_FOUND));
  }

  @Transactional
  public void updateSubscriptionPeriod(Invoice invoice, Subscription subscription) {
    List<Long> periods = invoice.getLines().getData().stream()
            .map(line -> line.getPeriod().getStart())
            .toList();

    if (!periods.isEmpty()) {
      subscription.setStartedAt(Instant.ofEpochSecond(Collections.min(periods)));
    }
  }


  @Transactional
  public void setAuditInfo(Subscription subscription, String agent) {
    CustomRevisionListener.setUsername(agent);
    subscription.setUsernameUpdate(agent);
  }

  @Transactional
  public com.stripe.model.Subscription getStripeSubscription(Subscription subscription) throws StripeException {
    validateStripeId(subscription.getStripeId());

    com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(subscription.getStripeId());
    validateStripeSubscription(stripeSubscription);

    return stripeSubscription;
  }

  @Transactional
  public void deleteSubscriptionFlow(Client client) {
    Optional.ofNullable(client.getSubscriptionFlow())
            .ifPresent(subscriptionFlow -> {
              subscriptionFlowRepository.deleteById(subscriptionFlow.getId());
              client.setSubscriptionFlow(null);
            });
  }

  private void validateStripeId(String stripeId) {
    if (ValidateDataUtils.isNullOrEmptyString(stripeId)) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_WITHOUT_STRIPE_ID);
    }
  }

  private void validateStripeSubscription(com.stripe.model.Subscription stripeSubscription) {
    if (stripeSubscription == null) {
      throw new ResourceNotFoundException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_FOUND_IN_STRIPE);
    }

    if (!"active".equals(stripeSubscription.getStatus())) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_NOT_ACTIVE_IN_STRIPE + stripeSubscription.getId());
    }
  }


}
