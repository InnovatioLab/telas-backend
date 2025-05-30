package com.telas.helpers;

import com.telas.dtos.response.MonitorValidationResponseDto;
import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Subscription;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
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

  private void validateCart(Cart cart) {
    if (cart.getItems().isEmpty()) {
      throw new BusinessRuleException(CartValidationMessages.CART_EMPTY);
    }

    if (!cart.isActive()) {
      throw new BusinessRuleException(CartValidationMessages.CART_INACTIVE);
    }
  }

  private void validateItems(List<CartItem> items) {
//    List<UUID> monitorIds = items.stream()
//            .map(item -> item.getMonitor().getId())
//            .toList();
//
//    Map<UUID, Monitor> monitors = monitorService.findAllByIds(monitorIds).stream()
//            .collect(Collectors.toMap(Monitor::getId, monitor -> monitor));
//
//    boolean hasInvalidMonitor = items.stream().anyMatch(item -> {
//      Monitor monitor = monitors.get(item.getMonitor().getId());
//      return monitor == null || !monitor.isActive() || !monitor.hasAvailableBlocks(item.getBlockQuantity()) ||
//             monitor.getClients().stream().anyMatch(client -> client.getId().equals(item.getCart().getClient().getId()));
//    });
//
//    if (hasInvalidMonitor) {
//      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE);
//    }
//
//    Client cartClient = items.get(0).getCart().getClient();
//
//    boolean hasAdsLinkedToClient = !attachmentHelper.findAdsByMonitorIdsAndClientId(monitorIds, cartClient.getId()).isEmpty();
//
//    if (hasAdsLinkedToClient) {
//      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_ALREADY_ATTACHED_TO_CLIENT);
//    }

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
}
