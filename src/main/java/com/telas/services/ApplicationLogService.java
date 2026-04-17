package com.telas.services;

import com.telas.dtos.request.BoxLogRequestDto;

import java.util.Map;
import java.util.UUID;

public interface ApplicationLogService {

    void persistFromHandler(String title, Throwable ex, int httpStatus);

    void persistBoxLog(BoxLogRequestDto request);

    void persistSystemLog(String level, String message, String source, Map<String, Object> metadata);

    void persistApiRequestLog(
        String httpMethod,
        String endpoint,
        UUID clientId,
        int httpStatus,
        Map<String, Object> metadata
    );
}
