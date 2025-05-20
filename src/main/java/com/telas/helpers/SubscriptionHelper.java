package com.telas.helpers;

import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.constants.valitation.MonitorValidationMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionHelper {
  private final CartService cartService;
  private final MonitorService monitorService;

  @Value("${monitor.block.price}")
  private String monitorBlockPrice;

  @Transactional
  public BigDecimal calculateTotalPrice(List<CartItem> items) {
    BigDecimal blockPrice = new BigDecimal(monitorBlockPrice);
    return items.stream()
            .map(item -> blockPrice.multiply(BigDecimal.valueOf(item.getBlockQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

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
    boolean hasInvalidMonitor = items.stream().anyMatch(item -> {
      Monitor monitor = monitorService.findEntityById(item.getMonitor().getId());
      return !monitor.isActive() || !monitor.hasAvailableBlocks(item.getBlockQuantity());
    });

    if (hasInvalidMonitor) {
      throw new BusinessRuleException(MonitorValidationMessages.MONITOR_INACTIVE_OR_BLOCKS_UNAVAILABLE);
    }
  }
}
