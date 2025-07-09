package com.telas.controllers.impl;

import com.telas.controllers.IpController;
import com.telas.dtos.response.ResponseDto;
import com.telas.services.IpService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "ips")
@RequiredArgsConstructor
public class IpControllerImpl implements IpController {
  private final IpService service;

  @Override
  @GetMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> findAll() {
    return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(service.findAll(), HttpStatus.OK, MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE));
  }
}
