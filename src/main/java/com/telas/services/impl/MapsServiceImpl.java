package com.telas.services.impl;

import com.telas.dtos.request.SearchNearbyRequestDto;
import com.telas.dtos.response.GeolocalizationApiResponse;
import com.telas.dtos.response.GeolocalizationResponseDto;
import com.telas.dtos.response.NearbySearchResponse;
import com.telas.entities.Address;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.repositories.AddressRepository;
import com.telas.services.MapsService;
import com.telas.shared.utils.HttpClientUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

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
    private final HttpClientUtil httpClientUtil;
    private final AddressRepository addressRepository;

    @Value("${geolocalizacao.service.url}")
    private String geolocalizacaoUrl;

    @Value("${geolocalizacao.service.key}")
    private String apiKey;


    @Override
    @Transactional
    public void getAddressCoordinates(Address address) {
        GeolocalizationResponseDto response = geolocationRequest(address);
        validateResponse(response);
        Double latitude = response.getLat();
        Double longitude = response.getLng();
        address.setLocation(latitude, longitude);
        addressRepository.save(address);
        getPlaceDetails(address, latitude, longitude);
    }

    private void getPlaceDetails(Address address, Double latitude, Double longitude) {
        SearchNearbyRequestDto requestDto = new SearchNearbyRequestDto(latitude, longitude);

        Map<String, String> PLACES_SEARCH_NEARBY_HEADERS = Map.of(
                "X-Goog-Api-Key", apiKey,
                "X-Goog-FieldMask", FIELD_MASK
        );

        NearbySearchResponse apiResponse = httpClientUtil.makePostRequestWithReturn(
                PLACES_SEARCH_NEARBY_URL, requestDto, NearbySearchResponse.class, null, PLACES_SEARCH_NEARBY_HEADERS
        );

        if (apiResponse != null && Objects.nonNull(apiResponse.getPlaces()) && !apiResponse.getPlaces().isEmpty()) {
            NearbySearchResponse.Place place = apiResponse.getPlaces().get(0);
            log.info("Place found: {}", place.getDisplayName());
            address.setLocationName(place.getDisplayName().getText() != null ? place.getDisplayName().getText() : null);
            address.setLocationDescription(place.getEditorialSummary() != null ? place.getEditorialSummary().getText() : null);
            addressRepository.save(address);
            getPhotosFromPlace(address, place);
        }
    }

    private void getPhotosFromPlace(Address address, NearbySearchResponse.Place place) {
        if (place.getPhotos() == null || place.getPhotos().isEmpty()) {
            log.warn("No photos available for place ID: {}", place.getId());
            return;
        }

        String photoUrl = String.format(PLACES_MEDIA_URL, place.getPhotos().get(0).getName(), apiKey);
        log.info("Fetching photo from URL: {}", photoUrl);

        try {
            HttpResponse<Void> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder().uri(URI.create(photoUrl)).GET().build(), HttpResponse.BodyHandlers.discarding());

            log.info("Response status code: {}", response.statusCode());

            if (response.statusCode() == 302) {
                String imageUrl = response.headers().firstValue("Location").orElse(null);
                log.info("Redirected to image URL: {}", imageUrl);
                address.setPhotoUrl(imageUrl);
                addressRepository.save(address);
            } else {
                log.warn("No photos available for place ID: {}", place.getId());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while fetching photo from place: {}", e.getMessage());
        }
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
