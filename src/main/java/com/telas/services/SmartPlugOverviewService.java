package com.telas.services;

import com.telas.dtos.response.SmartPlugHistoryPointResponseDto;
import com.telas.dtos.response.SmartPlugOverviewResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SmartPlugOverviewService {
    List<SmartPlugOverviewResponseDto> overview();

    List<SmartPlugHistoryPointResponseDto> history(UUID plugId, Instant from, Instant to, int limit);
}

