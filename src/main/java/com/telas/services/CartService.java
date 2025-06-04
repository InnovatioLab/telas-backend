package com.telas.services;

import com.telas.dtos.request.CartRequestDto;
import com.telas.dtos.response.CartResponseDto;
import com.telas.entities.Cart;

import java.util.UUID;

public interface CartService {
  CartResponseDto save(CartRequestDto request, UUID cartId);

  void inactivateCart(Cart cart);

  Cart findActiveByClientIdWithItens(UUID id);

  CartResponseDto findById(UUID id);
}

