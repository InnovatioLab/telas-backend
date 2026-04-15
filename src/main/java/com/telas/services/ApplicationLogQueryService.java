package com.telas.services;

import com.telas.dtos.response.ApplicationLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface ApplicationLogQueryService {

    Page<ApplicationLogResponseDto> findAll(
            String source,
            String level,
            Instant from,
            Instant to,
            String q,
            Pageable pageable);
}
