package com.marketingproject.services;

import com.marketingproject.dtos.EmailDataDto;

public interface EmailService {
    void send(EmailDataDto data);
}
