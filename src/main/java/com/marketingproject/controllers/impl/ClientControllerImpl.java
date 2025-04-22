package com.marketingproject.controllers.impl;

import com.marketingproject.controllers.ClientController;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.response.ResponseDto;
import com.marketingproject.services.ClientService;
import com.marketingproject.shared.constants.MessageCommonsConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "clients")
@RequiredArgsConstructor
public class ClientControllerImpl implements ClientController {
    private final ClientService service;

    @Override
    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody ClientRequestDto request) {
        service.save(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.SAVE_SUCCESS_MESSAGE));
    }
}
