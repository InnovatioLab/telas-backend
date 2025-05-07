package com.telas.dtos;


import com.telas.entities.Client;
import com.telas.entities.VerificationCode;
import com.telas.enums.ContactPreference;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class MessagingDataDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private final Client client;
    private final VerificationCode verificationCode;
    private final ContactPreference contactPreference;

    public MessagingDataDto(Client client, VerificationCode verificationCode, ContactPreference contactPreference) {
        this.client = client;
        this.verificationCode = verificationCode;
        this.contactPreference = contactPreference;
    }
}