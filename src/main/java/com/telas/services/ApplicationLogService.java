package com.telas.services;

import com.telas.dtos.request.BoxLogRequestDto;

public interface ApplicationLogService {

    void persistFromHandler(String title, Throwable ex, int httpStatus);

    void persistBoxLog(BoxLogRequestDto request);
}
