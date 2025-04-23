package com.marketingproject.services;

import com.marketingproject.dtos.request.MonitorRequestDto;

public interface GeolocationService {
    void getMonitorCoordinates(MonitorRequestDto request);
}
