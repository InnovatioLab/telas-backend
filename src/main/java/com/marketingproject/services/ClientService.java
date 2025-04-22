package com.marketingproject.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.marketingproject.dtos.request.ClientRequestDto;
import com.marketingproject.dtos.request.ContactRequestDto;
import com.marketingproject.dtos.request.PasswordRequestDto;
import com.marketingproject.entities.Client;

public interface ClientService {
    void save(ClientRequestDto request);

    Client findActiveByIdentification(String identification);

    void validateCode(String identification, String codigo);

    void resendCode(String identification);

    void updateContact(String identification, ContactRequestDto request) throws JsonProcessingException;

    void createPassword(String identification, PasswordRequestDto request);

    void resetPassword(PasswordRequestDto request);

    void sendResetPasswordCode(Client client);
}
