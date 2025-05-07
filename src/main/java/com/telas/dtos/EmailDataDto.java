package com.telas.dtos;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class EmailDataDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3963846843873646628L;

    private String email;
    private String template;
    private String subject;

    private Map<String, String> params = new HashMap<>();

    public EmailDataDto(MessagingDataDto messagingData, String template, String subject) {
        email = messagingData.getClient().getContact().getEmail();
        this.template = template;
        this.subject = subject;
        params.put("verificationCode", messagingData.getVerificationCode().getCode());
        params.put("name", messagingData.getClient().getBusinessName());
    }

    public EmailDataDto(String email, String template, String subject) {
        this.email = email;
        this.template = template;
        this.subject = subject;
    }
}