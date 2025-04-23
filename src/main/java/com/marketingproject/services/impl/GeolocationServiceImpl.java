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
        GeolocalizationResponseDto geolocalizationResponse = geolocationRequest(request);

        if (geolocalizationResponse == null) {
            log.error("Geolocation response is null");
            throw new BusinessRuleException("Error while fetching geolocation data");
        }

        request.setLatitude(geolocalizationResponse.getLat());
        request.setLongitude(geolocalizationResponse.getLng());
    }

    GeolocalizationResponseDto geolocationRequest(MonitorRequestDto request) {
        String param = request.getAddress().getCoordinatesParams();

        try {
            GeolocalizationApiResponse response = httpClientUtil.makeGetRequest(getGeolocalizacaoUrl(), GeolocalizationApiResponse.class,
                    Map.of(ADDRESS_PARAM, param, API_KEY_PARAM, getApiKey()));

            if (response != null && !response.getResults().isEmpty()) {
                GeolocalizationApiResponse.Location location = response.getResults().get(0).getGeometry().getLocation();
                return new GeolocalizationResponseDto(location.getLat(), location.getLng());
            }
            return null;
        } catch (Exception e) {
            log.error("Error while fetching geolocation data: {}", e.getMessage());
            throw new BusinessRuleException("Error while fetching geolocation data: " + e.getMessage());
        }
    }
}
