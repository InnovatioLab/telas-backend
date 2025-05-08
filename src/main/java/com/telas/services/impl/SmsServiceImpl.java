package com.telas.services.impl;

import com.telas.dtos.MessagingDataDto;
import com.telas.services.SmsService;
import com.telas.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    private final HttpClientUtil httpClientUtil;

    @Override
    public void send(MessagingDataDto data) {

    }
}
