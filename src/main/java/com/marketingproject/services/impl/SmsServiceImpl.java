package com.marketingproject.services.impl;

import com.marketingproject.dtos.MessagingDataDto;
import com.marketingproject.services.SmsService;
import com.marketingproject.shared.utils.HttpClientUtil;
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
