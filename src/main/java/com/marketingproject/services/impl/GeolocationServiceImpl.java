package com.marketingproject.services.impl;

import com.marketingproject.dtos.request.MonitorRequestDto;
import com.marketingproject.dtos.response.GeolocalizationApiResponse;
import com.marketingproject.dtos.response.GeolocalizationResponseDto;
import com.marketingproject.infra.exceptions.BusinessRuleException;
import com.marketingproject.services.GeolocationService;
import com.marketingproject.shared.utils.HttpClientUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
@Getter
public class GeolocationServiceImpl implements GeolocationService {
    private static final String API_KEY_PARAM = "key";
    private static final String ADDRESS_PARAM = "address";
    private final HttpClientUtil httpClientUtil;

    @Value("${geolocalizacao.service.url}")
    private String geolocalizacaoUrl;

    @Value("${geolocalizacao.service.key}")
    private String apiKey;

    @Override
    @Transactional
    public void getMonitorCoordinates(MonitorRequestDto request) {
        GeolocalizationResponseDto response = geolocationRequest(request);
        validateResponse(response);
        request.setLatitude(response.getLat());
        request.setLongitude(response.getLng());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Double> getCoordinatesFromZipCode(String zipCode, String countryCode) {
        GeolocalizationResponseDto response = geolocationRequest(zipCode, countryCode);
        validateResponse(response);
        return Map.of("latitude", response.getLat(), "longitude", response.getLng());
    }

    void validateResponse(GeolocalizationResponseDto response) {
        if (response == null) {
            log.error("Geolocation response is null");
            throw new BusinessRuleException("Error while fetching geolocation data");
        }
    }

    GeolocalizationResponseDto geolocationRequest(MonitorRequestDto request) {
        return fetchGeolocationData(request.getAddress().getCoordinatesParams());
    }

    GeolocalizationResponseDto geolocationRequest(String zipCode, String countryCode) {
        return fetchGeolocationData(zipCode + ", " + countryCode);
    }

    GeolocalizationResponseDto fetchGeolocationData(String param) {
        try {
            GeolocalizationApiResponse apiResponse = httpClientUtil.makeGetRequest(
                    geolocalizacaoUrl, GeolocalizationApiResponse.class,
                    Map.of(ADDRESS_PARAM, param, API_KEY_PARAM, apiKey)
            );

            if (apiResponse != null && !apiResponse.getResults().isEmpty()) {
                GeolocalizationApiResponse.Location location = apiResponse.getResults().get(0).getGeometry().getLocation();
                return new GeolocalizationResponseDto(location.getLat(), location.getLng());
            }
            return null;
        } catch (Exception e) {
            log.error("Error while fetching geolocation data: {}", e.getMessage());
            throw new BusinessRuleException("Error while fetching geolocation data: " + e.getMessage());
        }
    }
}
