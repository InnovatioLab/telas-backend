package com.marketingproject.services;

import com.marketingproject.dtos.MessagingDataDto;

public interface SmsService {
    void send(MessagingDataDto data);
}
