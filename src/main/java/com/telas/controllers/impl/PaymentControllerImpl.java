package com.telas.controllers.impl;

import com.telas.controllers.PaymentController;
import com.telas.dtos.request.UpdatePaymentStatusRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.PaymentService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "payments")
@RequiredArgsConstructor
public class PaymentControllerImpl implements PaymentController {
  private final PaymentService service;

  @Override
  @PatchMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> updateStatus(@Valid @RequestBody UpdatePaymentStatusRequestDto requestDto) {
    service.updatePaymentStatus(requestDto);
    return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
  }
}
