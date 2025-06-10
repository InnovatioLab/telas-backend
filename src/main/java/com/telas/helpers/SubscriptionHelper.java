package com.telas.helpers;

import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.telas.dtos.response.MonitorAdResponseDto;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.dtos.response.SubscriptionMonitorResponseDto;
import com.telas.dtos.response.SubscriptionResponseDto;
import com.telas.entities.*;
import com.telas.enums.Recurrence;
import com.telas.enums.SubscriptionStatus;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.BucketService;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.audit.CustomRevisionListener;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import com.telas.shared.utils.AttachmentUtils;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
  private final SubscriptionRepository repository;
  private final SubscriptionFlowRepository subscriptionFlowRepository;
  private final CartService cartService;
  private final MonitorService monitorService;
  private final BucketService bucketService;

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

  @Transactional(readOnly = true)
  public SubscriptionResponseDto getSubscriptionResponse(Subscription subscription, Client loggedUser) {
    SubscriptionResponseDto response = new SubscriptionResponseDto(subscription);
    List<Monitor> monitors = monitorService.findAllByIds(response.getMonitors().stream().map(SubscriptionMonitorResponseDto::getId).toList());

    if (monitors.isEmpty()) {
      return response;
    }

    Map<UUID, List<MonitorAdResponseDto>> monitorAdLinksMap = monitors.stream()
            .collect(Collectors.toMap(
                    Monitor::getId,
                    monitor -> monitor.getMonitorAds().stream()
                            .filter(monitorAd -> loggedUser.isAdmin() || monitorAd.getAd().getClient().getId().equals(loggedUser.getId()))
                            .map(monitorAd -> new MonitorAdResponseDto(monitorAd, bucketService.getLink(AttachmentUtils.format(monitorAd.getAd()))))
                            .toList()
            ));

    response.getMonitors().forEach(monitor ->
            monitor.setAdLinks(monitorAdLinksMap.getOrDefault(monitor.getId(), List.of()))
    );

    return response;
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

  @Transactional(readOnly = true)
  public void validateSubscriptionForUpgrade(Subscription entity) {
    if (entity.isBonus()) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_BONUS);
    }

    if (Recurrence.MONTHLY.equals(entity.getRecurrence())) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_MONTHLY);
    }

    boolean isExpired = entity.getEndsAt() != null && !entity.getEndsAt().isAfter(Instant.now());
    boolean isInactive = !SubscriptionStatus.ACTIVE.equals(entity.getStatus());

    if (isInactive || isExpired) {
      throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_NON_ACTIVE_OR_EXPIRED);
    }

    if (!Recurrence.MONTHLY.equals(entity.getRecurrence()) && entity.getStartedAt() != null && entity.getEndsAt() != null) {
//      long difference = entity.getEndsAt().getEpochSecond() - entity.getStartedAt().getEpochSecond();
//
//      if (difference > SharedConstants.MAX_BILLING_CYCLE_ANCHOR) {
//        throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_SHORT_BILLING_CYCLE);
//      }

      long remainingTime = entity.getEndsAt().getEpochSecond() - Instant.now().getEpochSecond();

      if (remainingTime > SharedConstants.MAX_BILLING_CYCLE_ANCHOR) {
        throw new BusinessRuleException(SubscriptionValidationMessages.SUBSCRIPTION_UPGRADE_NOT_ALLOWED_FOR_SHORT_BILLING_CYCLE);
      }
    }
  }
}
