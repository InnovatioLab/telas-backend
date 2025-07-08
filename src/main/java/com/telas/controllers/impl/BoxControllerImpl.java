package com.telas.controllers.impl;

import com.telas.controllers.BoxController;
import com.telas.dtos.request.BoxRequestDto;
import com.telas.dtos.response.ResponseDto;
import com.telas.dtos.response.StatusMonitorsResponseDto;
import com.telas.services.BoxService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "boxes")
@RequiredArgsConstructor
public class BoxControllerImpl implements BoxController {
  private final BoxService service;

  @Override
  @PostMapping
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> save(@Valid @RequestBody BoxRequestDto request) {
    service.save(request, null);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
  }

  @Override
  @PutMapping("/{id}")
  @SecurityRequirement(name = "jwt")
  public ResponseEntity<?> update(@Valid @RequestBody BoxRequestDto request, @PathVariable(name = "id") UUID boxId) {
    service.save(request, boxId);
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
  }

  @Override
  @GetMapping
  public ResponseEntity<?> getMonitorsAdsByIp(@RequestHeader("X-Box-Ip") String ip) {
    return ResponseEntity.status(HttpStatus.OK)
            .body(ResponseDto.fromData(service.getMonitorsAdsByIp(ip), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
  }

  @Override
  @PostMapping("/health")
  public ResponseEntity<?> checkMonitorsHealth(@RequestBody List<StatusMonitorsResponseDto> responseList) {
    service.checkMonitorsHealth(responseList);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(ResponseDto.fromData(null, HttpStatus.NO_CONTENT, null));
  }
}
