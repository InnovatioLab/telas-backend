package com.telas.services.impl;


import com.telas.dtos.request.CartItemRequestDto;
import com.telas.dtos.request.CartRequestDto;
import com.telas.dtos.response.CartItemResponseDto;
import com.telas.dtos.response.CartResponseDto;
import com.telas.entities.*;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.CartItemRepository;
import com.telas.repositories.CartRepository;
import com.telas.repositories.SubscriptionFlowRepository;
import com.telas.services.CartService;
import com.telas.services.MonitorService;
import com.telas.shared.constants.SharedConstants;
import com.telas.shared.constants.valitation.CartValidationMessages;
import com.telas.shared.utils.ValidateDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
  private final CartRepository repository;
  private final CartItemRepository cartItemRepository;
  private final SubscriptionFlowRepository subscriptionFlowRepository;
  private final AuthenticatedUserService authenticatedUserService;
  private final MonitorService monitorService;

  @Override
  @Transactional
  public CartResponseDto save(CartRequestDto request, UUID cartId) {
    Client client = authenticatedUserService.getLoggedUser().client();

    Cart cart = (cartId != null) ? findEntityById(cartId) : null;

    if (cart == null) {
      if (repository.findActiveByClientIdWithItens(client.getId()).isPresent()) {
        throw new ResourceNotFoundException(CartValidationMessages.CART_ALREADY_EXISTS);
      }

      cart = new Cart(client);
      cart.setRecurrence(request.getRecurrence());
      repository.save(cart);

      if (client.getSubscriptionFlow() == null) {
        subscriptionFlowRepository.save(new SubscriptionFlow(client));
      }
    }

    cart.setRecurrence(request.getRecurrence());
    List<CartItemResponseDto> itemsResponse = saveCartItems(request, cart);
    repository.save(cart);
    return getCartResponse(cart, itemsResponse);
  }

  @Override
  @Transactional
  public void inactivateCart(Cart cart) {
    if (cart.isActive()) {
      cart.inactivate();
      repository.save(cart);
    }
  }

  @Override
  @Transactional
  public Cart findByClientIdWithItens(UUID id) {
    return repository.findByClientIdWithItens(id).orElseThrow(() -> new ResourceNotFoundException(CartValidationMessages.CART_NOT_FOUND));
  }

  @Override
  @Transactional
  public CartResponseDto findById(UUID id) {
    Cart cart = findEntityById(id);
    return getCartResponse(cart, null);
  }

  @Override
  @Transactional
  public void deleteCart(Cart cart) {
    if (cart == null) {
      throw new ResourceNotFoundException(CartValidationMessages.CART_NOT_FOUND);
    }

    if (cart.isActive()) {
      throw new ResourceNotFoundException(CartValidationMessages.CART_ACTIVE);
    }

    repository.delete(cart);

  }

  private CartResponseDto getCartResponse(Cart cart, List<CartItemResponseDto> itemsResponse) {
    CartResponseDto response = new CartResponseDto(cart);

    if (!ValidateDataUtils.isNullOrEmpty(itemsResponse)) {
      response.setItems(itemsResponse);
    }

    return response;
  }

  private List<CartItemResponseDto> saveCartItems(CartRequestDto request, Cart cart) {
    Map<UUID, CartItem> actualItems = mapExistingCartItems(cart);
    List<CartItemResponseDto> itemsResponse = new ArrayList<>();

    Set<UUID> itemsReceivedIds = processReceivedItems(request, cart, actualItems, itemsResponse);
    removeUnreceivedItems(cart, actualItems, itemsReceivedIds);
    deleteCartIfEmpty(cart);
    return itemsResponse;
  }

  private Map<UUID, CartItem> mapExistingCartItems(Cart cart) {
    List<CartItem> cartItems = !ValidateDataUtils.isNullOrEmpty(cart.getItems()) ? cart.getItems() : new ArrayList<>();
    return cartItems.stream().collect(Collectors.toMap(item -> item.getMonitor().getId(), item -> item));
  }

  private Set<UUID> processReceivedItems(CartRequestDto request, Cart cart, Map<UUID, CartItem> actualItems, List<CartItemResponseDto> itemsResponse) {
    Set<UUID> itemsReceivedIds = new HashSet<>();

    for (CartItemRequestDto itemDto : request.getItems()) {
      UUID monitorId = itemDto.getMonitorId();
      Monitor monitor = monitorService.findEntityById(monitorId);

      CartItem cartItem = actualItems.computeIfAbsent(monitorId, k -> new CartItem(cart, monitor, itemDto));
      cartItem.setBlockQuantity(itemDto.getBlockQuantity());
      cartItemRepository.save(cartItem);

      itemsResponse.add(new CartItemResponseDto(cartItem));

      itemsReceivedIds.add(monitorId);
    }

    return itemsReceivedIds;
  }

  private void removeUnreceivedItems(Cart cart, Map<UUID, CartItem> actualItems, Set<UUID> itemsReceivedIds) {
    List<CartItem> itemsToRemove = actualItems.values().stream()
            .filter(item -> !itemsReceivedIds.contains(item.getMonitor().getId()))
            .toList();

    if (!itemsToRemove.isEmpty()) {
      cartItemRepository.deleteAll(itemsToRemove);
      cartItemRepository.flush();

      if (cart.getItems() != null && !cart.getItems().isEmpty()) {
        cart.getItems().removeAll(itemsToRemove);
      }
    }
  }

  private void deleteCartIfEmpty(Cart cart) {
    if (cartItemRepository.countByCartId(cart.getId()) == SharedConstants.ZERO) {
      repository.delete(cart);
    }
  }

  private Cart findEntityById(UUID cartId) {
    return repository.findByIdWithItens(cartId)
            .orElseThrow(() -> new ResourceNotFoundException(CartValidationMessages.CART_NOT_FOUND));
  }
}
