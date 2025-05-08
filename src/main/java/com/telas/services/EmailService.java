package com.telas.services;

import com.telas.dtos.EmailDataDto;

public interface EmailService {
    void send(EmailDataDto data);
}
