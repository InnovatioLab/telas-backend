package com.marketingproject.services;

import com.marketingproject.dtos.request.MonitorRequestDto;

import java.util.Map;

public interface GeolocationService {
    void getMonitorCoordinates(MonitorRequestDto request);

    Map<String, Double> getCoordinatesFromZipCode(String zipCode);
}
