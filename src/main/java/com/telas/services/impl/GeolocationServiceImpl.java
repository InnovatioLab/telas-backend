package com.telas.services.impl;

import com.telas.dtos.response.GeolocalizationApiResponse;
import com.telas.dtos.response.GeolocalizationResponseDto;
import com.telas.entities.Address;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.GeolocationService;
import com.telas.shared.utils.HttpClientUtil;
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
    public void getAddressCoordinates(Address address) {
        GeolocalizationResponseDto response = geolocationRequest(address);
        validateResponse(response);
        address.setLocation(response.getLat(), response.getLng());
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

    GeolocalizationResponseDto geolocationRequest(Address address) {
        return fetchGeolocationData(address.getCoordinatesParams());
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
