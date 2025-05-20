package com.telas.helpers;

import com.telas.dtos.response.CartItemResponseDto;
import com.telas.dtos.response.CartResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartHelper {

  @Value("${monitor.block.price}")
  private String monitorBlockPrice;

  @Transactional
  public void setCartResponseTotalPrice(CartResponseDto cartResponse) {
    List<CartItemResponseDto> items = cartResponse.getItems();
    BigDecimal blockPrice = new BigDecimal(monitorBlockPrice);

    if (items.isEmpty()) {
      cartResponse.setTotalPrice(BigDecimal.ZERO);
      return;
    }

    items.forEach(item -> {
      item.setPrice(blockPrice.multiply(BigDecimal.valueOf(item.getBlockQuantity())));
    });

    cartResponse.setTotalPrice(
            items.stream()
                    .map(CartItemResponseDto::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
    );

  }
}
