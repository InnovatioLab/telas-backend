package com.telas.services;

import com.telas.dtos.request.BoxLogRequestDto;

import java.util.Map;

public interface ApplicationLogService {

    void persistFromHandler(String title, Throwable ex, int httpStatus);

    void persistBoxLog(BoxLogRequestDto request);

    void persistSystemLog(String level, String message, String source, Map<String, Object> metadata);
}
