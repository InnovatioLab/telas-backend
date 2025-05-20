package com.telas.dtos.response;

import com.telas.entities.Cart;
import com.telas.enums.Recurrence;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public final class CartResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private UUID id;

  private boolean active;

  private Recurrence recurrence;

  private BigDecimal totalPrice;

  private List<CartItemResponseDto> items;

  public CartResponseDto(Cart entity) {
    id = entity.getId();
    active = entity.isActive();
    recurrence = entity.getRecurrence();
    items = entity.getItems().stream()
            .map(CartItemResponseDto::new)
            .toList();
  }
}
