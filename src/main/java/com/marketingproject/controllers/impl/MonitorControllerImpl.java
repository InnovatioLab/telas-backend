package com.marketingproject.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.controllers.MonitorController;
import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.dtos.response.ResponseDto;
import com.marketingproject.services.MonitorService;
import com.marketingproject.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "monitors")
@RequiredArgsConstructor
public class MonitorControllerImpl implements MonitorController {
    private final MonitorService service;

    @Override
    @PostMapping
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> save(@Valid @RequestBody MonitorRequestDto request) throws JsonProcessingException {
        service.save(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }

    @Override
    @PutMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@Valid @RequestBody MonitorRequestDto request, @PathVariable(name = "id") UUID monitorId) throws JsonProcessingException {
        service.save(request, monitorId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findById(@PathVariable(name = "id") UUID monitorId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findById(monitorId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }
}
