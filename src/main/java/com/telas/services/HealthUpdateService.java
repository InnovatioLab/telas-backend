package com.telas.services;

import com.telas.dtos.request.StatusBoxMonitorsRequestDto;

public interface HealthUpdateService {

    void applyHealthUpdate(StatusBoxMonitorsRequestDto request);
}
