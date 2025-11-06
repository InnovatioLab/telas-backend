package com.telas.controllers.impl;

import com.telas.controllers.CartController;
import com.telas.dtos.request.CartRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.CartService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "carts")
@RequiredArgsConstructor
public class CartControllerImpl implements CartController {
    private final CartService service;

    @Override
    @PostMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> save(@Valid @RequestBody CartRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(service.save(request, null), HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @Override
    @PutMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@RequestBody CartRequestDto request, @PathVariable(name = "id") UUID cartId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.save(request, cartId), HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findById(@PathVariable(name = "id") UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findById(id), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getLoggedUserCart() {
        Object response = service.getLoggedUserCart();
        String message = response != null ? MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE : null;

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(response, HttpStatus.OK, message));
    }
}
