package com.telas.services;

import com.telas.dtos.MessagingDataDto;

public interface SmsService {
    void send(MessagingDataDto data);
}
