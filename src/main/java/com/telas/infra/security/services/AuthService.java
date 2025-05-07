package com.telas.infra.security.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telas.infra.security.model.LoginRequestDto;
import com.telas.infra.security.model.PasswordRequestDto;
import com.telas.infra.security.model.PasswordUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

public interface AuthService {
    String login(@Validated LoginRequestDto requestDto) throws JsonProcessingException;

    void sendPasswordRecoveryCode(String identificationNumber);

    void resetPassword(String identificationNumber, @Valid PasswordRequestDto request);

    void updatePassword(@Valid PasswordUpdateRequestDto request);
}
