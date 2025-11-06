package com.telas.infra.security.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.dtos.request.LoginRequestDto;
import com.telas.dtos.request.PasswordRequestDto;
import com.telas.dtos.request.PasswordUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

public interface AuthService {
    String login(@Validated LoginRequestDto requestDto) throws JsonProcessingException;

    void sendPasswordRecoveryCode(String email);

    void resetPassword(String email, @Valid PasswordRequestDto request);

    void updatePassword(@Valid PasswordUpdateRequestDto request);
}
