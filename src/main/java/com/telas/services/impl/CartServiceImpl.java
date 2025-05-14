package com.telas.services.impl;


import com.telas.dtos.request.CartItemRequestDto;
import com.telas.dtos.request.CartRequestDto;
import com.telas.entities.Cart;
import com.telas.entities.CartItem;
import com.telas.entities.Client;
import com.telas.entities.Monitor;
import com.telas.infra.exceptions.ResourceNotFoundException;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.repositories.CartItemRepository;
import com.telas.repositories.CartRepository;
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
    private final AuthenticatedUserService authenticatedUserService;
    private final MonitorService monitorService;

    @Override
    @Transactional
    public void save(CartRequestDto request, UUID cartId) {
        Client client = authenticatedUserService.getLoggedUser().client();

        Cart cart = (cartId != null) ? findEntityById(cartId) : null;

        if (cart == null) {
            if (repository.findByClientIdWithItens(client.getId()).isPresent()) {
                throw new ResourceNotFoundException(CartValidationMessages.CART_ALREADY_EXISTS);
            }

            cart = new Cart(client);
        }

        saveCartItems(request, cart);
        repository.save(cart);
    }

    @Override
    @Transactional
    public void inactivateCart(Cart cart) {
        cart.inactivate();
        repository.save(cart);
    }

    private void saveCartItems(CartRequestDto request, Cart cart) {
        List<CartItem> cartItems = !ValidateDataUtils.isNullOrEmpty(cart.getItems()) ? cart.getItems() : new ArrayList<>();

        Map<UUID, CartItem> actualItems = cartItems.stream().collect(Collectors.toMap(item -> item.getMonitor().getId(), item -> item));

        Set<UUID> itemsReceivedIds = new HashSet<>();

        updateOrCreateNewItem(request, cart, actualItems, itemsReceivedIds);

        List<CartItem> itensParaRemover = actualItems.values().stream()
                .filter(item -> !itemsReceivedIds.contains(item.getMonitor().getId()))
                .toList();

        cartItemRepository.deleteAll(itensParaRemover);
        cartItemRepository.flush();

        if (!ValidateDataUtils.isNullOrEmpty(cart.getItems())) {
            cart.getItems().removeAll(itensParaRemover);
        }

        if (cartItemRepository.countByCartId(cart.getId()) == SharedConstants.ZERO) {
            repository.delete(cart);
//            return Collections.emptyList();
        }

//        return cart.getItems();
    }

    private void updateOrCreateNewItem(CartRequestDto request, Cart cart, Map<UUID, CartItem> actualItems, Set<UUID> itemsReceivedIds) {
        for (CartItemRequestDto itemDto : request.getItems()) {
            UUID monitorId = itemDto.getMonitorId();
            Monitor monitor = monitorService.findEntityById(monitorId);

//            Verificar se o monitor ainda tem "vaga"
            CartItem cartItem = actualItems.computeIfAbsent(monitorId, k -> new CartItem(cart, monitor, itemDto));
            cartItemRepository.save(cartItem);

            itemsReceivedIds.add(monitorId);
        }
    }

    private Cart findEntityById(UUID cartId) {
        return repository.findByIdWithItens(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(CartValidationMessages.CART_NOT_FOUND));
    }
}
