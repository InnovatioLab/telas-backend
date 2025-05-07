package com.telas.services;

import com.telas.dtos.request.MonitorRequestDto;

import java.util.Map;

public interface GeolocationService {
    void getMonitorCoordinates(MonitorRequestDto request);

    Map<String, Double> getCoordinatesFromZipCode(String zipCode, String countryCode);
}
