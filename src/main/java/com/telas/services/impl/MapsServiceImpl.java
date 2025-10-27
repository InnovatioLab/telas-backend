package com.telas.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.dtos.request.SearchNearbyRequestDto;
import com.telas.dtos.response.GeolocalizationApiResponse;
import com.telas.dtos.response.GeolocalizationResponseDto;
import com.telas.dtos.response.NearbySearchResponse;
import com.telas.entities.Address;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.services.AddressService;
import com.telas.services.MapsService;
import com.telas.shared.utils.HttpClientUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
@Getter
public class MapsServiceImpl implements MapsService {
    private static final String API_KEY_PARAM = "key";
    private static final String ADDRESS_PARAM = "address";
    private static final String PLACES_SEARCH_NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String PLACES_MEDIA_URL = "https://places.googleapis.com/v1/%s/media?maxWidthPx=1024&key=%s";
    private static final String FIELD_MASK = "places.displayName,places.editorialSummary,places.photos";
    private static final String STREET_VIEW_METADATA_URL = "https://maps.googleapis.com/maps/api/streetview/metadata?location=%s,%s&key=%s";
    private static final String STREET_VIEW_IMAGE_URL = "https://maps.googleapis.com/maps/api/streetview?size=1024x1024&location=%s,%s&key=%s";
    private final HttpClientUtil httpClientUtil;
    private final AddressService addressService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${geolocalizacao.service.url}")
    private String geolocalizacaoUrl;

    @Value("${geolocalizacao.service.key}")
    private String apiKey;

    @Override
    public void getAddressCoordinates(Address address) {
        GeolocalizationResponseDto response = geolocationRequest(address);
        validateResponse(response);
        Double latitude = response.getLat();
        Double longitude = response.getLng();
        NearbySearchResponse.Place place = fetchPlaceDetails(latitude, longitude);
        String photoUrl = fetchPhotoUrl(place, latitude, longitude);
        saveAddressUpdates(address, latitude, longitude, place, photoUrl);
    }

    private void saveAddressUpdates(Address address, Double latitude, Double longitude,
                                    NearbySearchResponse.Place place, String photoUrl) {
        log.info("Saving updates for address: {}", address.getId());
        addressService.saveAddressUpdates(address, latitude, longitude, place, photoUrl);
        log.info("Address {} updated successfully with location and photo.", address.getId());
    }

    private NearbySearchResponse.Place fetchPlaceDetails(Double latitude, Double longitude) {
        SearchNearbyRequestDto requestDto = new SearchNearbyRequestDto(latitude, longitude);
        Map<String, String> headers = Map.of("X-Goog-Api-Key", apiKey, "X-Goog-FieldMask", FIELD_MASK);

        NearbySearchResponse apiResponse = httpClientUtil.makePostRequestWithReturn(
                PLACES_SEARCH_NEARBY_URL, requestDto, NearbySearchResponse.class, null, headers);

        if (apiResponse != null && apiResponse.getPlaces() != null && !apiResponse.getPlaces().isEmpty()) {
            NearbySearchResponse.Place place = apiResponse.getPlaces().get(0);
            log.info("Place found: {}", place.getDisplayName().getText());
            return place;
        } else {
            log.warn("No places found near coordinates: {},{}", latitude, longitude);
            return null;
        }
    }

    private String fetchPhotoUrl(NearbySearchResponse.Place place, Double latitude, Double longitude) {
        if (place != null && place.getPhotos() != null && !place.getPhotos().isEmpty()) {
            String photoName = place.getPhotos().get(0).getName();
            String photoApiUrl = String.format(PLACES_MEDIA_URL, photoName, apiKey);
            log.info("Fetching photo from URL: {}", photoApiUrl);

            try {
                HttpResponse<Void> response = httpClient.send(
                        HttpRequest.newBuilder().uri(URI.create(photoApiUrl)).GET().build(),
                        HttpResponse.BodyHandlers.discarding()
                );

                if (response.statusCode() == 302) {
                    String imageUrl = response.headers().firstValue("Location").orElse(null);
                    if (imageUrl != null) {
                        log.info("Place photo URL found (via redirect): {}", imageUrl);
                        return imageUrl; // Sucesso!
                    }
                }
                log.warn("No photo URL redirect found for place ID: {} (status code: {})", place.getId(), response.statusCode());

            } catch (IOException | InterruptedException e) {
                log.error("Error while fetching photo from place: {}. Falling back to Street View.", e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        } else if (place != null) {
            log.warn("No photos array available for place ID: {}", place.getId());
        }

        log.info("Trying Street View fallback for coordinates: {},{}", latitude, longitude);
        return fetchStreetViewPhotoUrl(latitude, longitude);
    }

    private String fetchStreetViewPhotoUrl(Double latitude, Double longitude) {
        try {
            String metadataUrl = String.format(STREET_VIEW_METADATA_URL, latitude, longitude, apiKey);

            HttpResponse<String> metaResponse = httpClient.send(
                    HttpRequest.newBuilder().uri(URI.create(metadataUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (metaResponse.statusCode() == 200) {
                String status = objectMapper.readTree(metaResponse.body()).path("status").asText();

                if ("OK".equalsIgnoreCase(status)) {
                    String streetViewUrl = String.format(STREET_VIEW_IMAGE_URL, latitude, longitude, apiKey);
                    log.info("Street View photo URL found: {}", streetViewUrl);
                    return streetViewUrl;
                }
                log.warn("Street View metadata status: {}", status);
            } else {
                log.warn("Street View metadata request failed with status: {}", metaResponse.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while fetching Street View metadata: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        log.warn("Could not find any photo URL.");
        return null;
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
