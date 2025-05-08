package com.telas.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.controllers.ClientController;
import com.telas.dtos.request.*;
import com.telas.dtos.request.filters.ClientFilterRequestDto;
import com.telas.dtos.request.filters.FilterAdRequestDto;
import com.telas.dtos.response.*;
import com.telas.enums.AdValidationType;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.services.ClientService;
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
    @PostMapping("/request-ad")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> requestAdCreation(@Valid @RequestBody ClientAdRequestToAdminDto request) {
        service.requestAdCreation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.REQUEST_AD_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping("/ads")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> uploadAd(@Valid @RequestBody AdRequestDto request) {
        service.uploadAds(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.UPLOAD_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/filters")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findAllFilters(ClientFilterRequestDto request) {
        PaginationResponseDto<List<ClientMinResponseDto>> response = service.findAllFilters(request);

        String msg = response.getList().isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;

        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(response, HttpStatus.OK, msg));
    }

    @Override
    @PatchMapping("/accept-terms-conditions")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> acceptTermsConditions() {
        service.acceptTermsAndConditions();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ResponseDto.fromData(null, HttpStatus.ACCEPTED, MessageCommonsConstants.ACCEPT_TERMS_CONDITIONS_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/partner/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> changeRoleToPartner(@PathVariable(name = "id") UUID clientId) throws JsonProcessingException {
        service.changeRoleToPartner(clientId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.UPDATE_SUCCESS_MESSAGE));
    }

    @Override
    @GetMapping("/ads-requests")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findAdRequestsByFilter(FilterAdRequestDto request) {
        PaginationResponseDto<List<AdRequestResponseDto>> response = service.findPendingAdRequest(request);

        String msg = response.getList().isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;

        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(response, HttpStatus.OK, msg));
    }

    @Override
    @GetMapping("/pending-ads")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> findPendingAds() {
        List<AdResponseDto> response = service.findPendingAds();
        String msg = response.isEmpty() ? MessageCommonsConstants.FIND_FILTER_EMPTY_MESSAGE : MessageCommonsConstants.FIND_ALL_SUCCESS_MESSAGE;
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(response, HttpStatus.OK, msg));
    }

    @Override
    @PatchMapping("/validate-ad/{id}")
    @SecurityRequirement(name = "jwt")
    public ResponseEntity<?> validateAd(
            @RequestParam("validation") AdValidationType validation,
            @RequestBody(required = false) RefusedAdRequestDto request,
            @PathVariable(name = "id") UUID adId) throws JsonProcessingException {

        service.validateAd(adId, validation, request);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.ATTACHMENT_VALIDATION_MESSAGE));
    }


}
