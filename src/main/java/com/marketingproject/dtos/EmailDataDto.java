package com.marketingproject.dtos;


import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class EmailDataDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private final String email;
    private final String template;
    private final String subject;

    private Map<String, String> params = new HashMap<>();

    public EmailDataDto(MessagingDataDto messagingData, String template, String subject) {
        email = messagingData.getClient().getContact().getEmail();
        this.template = template;
        this.subject = subject;
        params.put("verificationCode", messagingData.getVerificationCode().getCode());
        params.put("name", messagingData.getClient().getBusinessName());
    }
}