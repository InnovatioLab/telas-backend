package com.marketingproject.dtos;


import com.marketingproject.entities.Client;
import com.marketingproject.entities.VerificationCode;
import com.marketingproject.enums.ContactPreference;
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