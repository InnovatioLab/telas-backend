package com.telas.services;

import com.telas.dtos.MessagingDataDto;
import com.telas.entities.Client;
import com.telas.entities.VerificationCode;
import com.telas.enums.CodeType;

public interface VerificationCodeService {
    VerificationCode save(CodeType type, Client client);

    void validate(Client client, String code);

    void send(MessagingDataDto messagingDataDto, String template, String subject);
}
