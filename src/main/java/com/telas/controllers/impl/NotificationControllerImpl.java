package com.telas.controllers.impl;

import com.telas.controllers.NotificationController;
import com.telas.dtos.request.NotificationRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.NotificationService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "notifications")
@RequiredArgsConstructor
public class NotificationControllerImpl implements NotificationController {
  private final NotificationService service;

  @Override
  @GetMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> listClientNotifications(@RequestBody NotificationRequestDto request) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.listClientNotifications(request), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
  }

  @Override
  @GetMapping("/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findById(@PathVariable(name = "id") UUID notificationId) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.findById(notificationId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
  }
}
