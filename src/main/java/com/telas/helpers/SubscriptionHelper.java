package com.telas.helpers;

import com.stripe.model.Invoice;
import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.enums.Recurrence;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import com.telas.shared.constants.valitation.SubscriptionValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
  private final SubscriptionRepository repository;
  private final CartService cartService;
  private final MonitorService monitorService;

  @Transactional
  public Cart getActiveCart(Client client) {
    Cart cart = cartService.findByClientIdWithItens(client.getId());

    validateCart(cart);
    validateItems(cart.getItems());
    return cart;
  }

  @Transactional
  public void inactivateCart(Cart cart) {
    cartService.inactivateCart(cart);
  }

  @Transactional
  public void deleteCart(Cart cart) {
    cartService.deleteCart(cart);
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

    List<MonitorValidationResponseDto> results = monitorService.findInvalidMonitorsOrLinkedAds(monitorIds, client.getId());

    // Verifica se algum monitor é inválido
    boolean hasInvalidMonitor = results.stream().anyMatch(result -> !result.isValidMonitor());

    if (hasInvalidMonitor) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE);
    }

    // Verifica se há anúncios vinculados
    boolean hasAdsLinkedToClient = results.stream().anyMatch(MonitorValidationResponseDto::isHasLinkedAd);

    if (hasAdsLinkedToClient) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_ALREADY_ATTACHED_TO_CLIENT);
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
            .flatMap(line -> Stream.of(line.getPeriod().getStart(), line.getPeriod().getEnd()))
            .toList();

    if (periods.isEmpty()) {
      return;
    }

    Long startPeriod = Collections.min(periods);
    Long endPeriod = Collections.max(periods);

    if (subscription.getStartedAt() == null) {
      subscription.setStartedAt(Instant.ofEpochSecond(startPeriod));
    }

    Instant endAt = startPeriod.equals(endPeriod)
            ? Recurrence.MONTHLY.calculateEndsAt(subscription.getStartedAt())
            : Instant.ofEpochSecond(endPeriod);

    subscription.setEndsAt(endAt);
  }
}
