package com.telas.services;

import com.telas.dtos.request.CartRequestDto;
import com.telas.entities.Cart;

import java.util.UUID;

public interface CartService {
  void save(CartRequestDto request, UUID cartId);

  void inactivateCart(Cart cart);

  Cart findByClientIdWithItens(UUID id);
}

