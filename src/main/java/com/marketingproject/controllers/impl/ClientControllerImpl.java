package com.marketingproject.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.controllers.ClientController;
import com.marketingproject.dtos.request.AdvertisingAttachmentRequestDto;
import com.marketingproject.dtos.request.AttachmentRequestDto;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.dtos.response.ResponseDto;
import com.marketingproject.infra.security.model.PasswordRequestDto;
import com.marketingproject.services.ClientService;
import com.marketingproject.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @Override
    @PatchMapping("/validate-code/{identification}")
    public ResponseEntity<?> validateCode(@PathVariable(name = "identification") String identification, @RequestParam(name = "code") String code) {
        service.validateCode(identification, code);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.CODE_CONFIRMED_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/resend-code/{identification}")
    public ResponseEntity<?> resendCode(@PathVariable(name = "identification") String identification) {
        service.resendCode(identification);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.CODE_SENT_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/update-contact/{identification}")
    public ResponseEntity<?> updateContact(@PathVariable(name = "identification") String identification, @Valid @RequestBody ContactRequestDto request) throws JsonProcessingException {
        service.updateContact(identification, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/create-password/{identification}")
    public ResponseEntity<?> createPassword(@PathVariable(name = "identification") String identification, @Valid @RequestBody PasswordRequestDto request) {
        service.createPassword(identification, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, "Password created successfully!"));
    }

    @Override
    @GetMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findById(@PathVariable(name = "id") UUID clientId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.findById(clientId), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/authenticated")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> getDataFromToken() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(service.getDataFromToken(), HttpStatus.OK, MessageCommonsConstants.FIND_ID_SUCCESS_MESSAGE));
    }

    @Override
    @PutMapping("/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> update(@Valid @RequestBody ClientRequestDto request, @PathVariable(name = "id") UUID clientId) throws JsonProcessingException {
        service.update(request, clientId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/attachments/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> uploadAttachments(@Valid @RequestBody List<AttachmentRequestDto> request, @PathVariable(name = "id") UUID clientId) throws JsonProcessingException {
        service.uploadAttachments(request, clientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.UPLOAD_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/advertising-attachments/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> uploadAdvertisingAttachments(@Valid @RequestBody List<AdvertisingAttachmentRequestDto> request, @PathVariable(name = "id") UUID clientId) throws JsonProcessingException {
        service.uploadAdvertisingAttachments(request, clientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.UPLOAD_SUCCESS_MESSAGE));
    }
}
