package com.telas.controllers.impl;

import com.telas.controllers.SubscriptionController;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.SubscriptionService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "subscriptions")
@RequiredArgsConstructor
public class SubscriptionControllerImpl implements SubscriptionController {
  private final SubscriptionService service;

  @Override
  @PostMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> save() {
    return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.fromData(service.save(), HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
  }


  @Override
  @GetMapping("/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findById(@PathVariable(name = "id") UUID subscriptionId) {
    return null;
  }
}
