package com.telas.infra.security.controllers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.controllers.AuthController;
import com.telas.infra.security.model.LoginRequestDto;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import com.telas.infra.security.services.AuthService;
import com.telas.shared.constants.MessageCommonsConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "auth")
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {
    private final AuthService authService;

    @Override
    @PostMapping(value = "/login")
    public ResponseEntity<ResponseDto<String>> login(@Valid @RequestBody LoginRequestDto loginRequestDto) throws JsonProcessingException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.fromData(authService.login(loginRequestDto), HttpStatus.CREATED, MessageCommonsConstants.LOGIN_SUCCESS_MESSAGE));
    }

    @Override
    @PostMapping(value = "/recovery-password/{identificationNumber}")
    public ResponseEntity<?> sendPasswordRecoveryCode(@PathVariable(name = "identificationNumber") String identificationNumber) {
        authService.sendPasswordRecoveryCode(identificationNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.fromData(null, HttpStatus.CREATED, MessageCommonsConstants.CODE_SENT_SUCCESS_MESSAGE));
    }

    @Override
    @SecurityRequirement(name = "jwt")
    @PatchMapping("/update-password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody PasswordUpdateRequestDto request) {
        authService.updatePassword(request);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.PASSWORD_CHANGED_SUCCESS_MESSAGE));
    }

    @Override
    @PatchMapping("/reset-password/{identificationNumber}")
    public ResponseEntity<?> resetPassword(@PathVariable(name = "identificationNumber") String identificationNumber, @Valid @RequestBody PasswordRequestDto request) {
        authService.resetPassword(identificationNumber, request);
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.fromData(null, HttpStatus.OK, MessageCommonsConstants.PASSWORD_RESET_SUCCESS_MESSAGE));
    }
}
