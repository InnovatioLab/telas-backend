package com.marketingproject.services;

import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.entities.Client;
import com.marketingproject.entities.VerificationCode;
import com.marketingproject.enums.CodeType;

public interface VerificationCodeService {
    VerificationCode save(CodeType type, Client client);

    void validate(Client client, String code);

    void send(MessagingDataDto messagingDataDto, String template, String subject);
}
