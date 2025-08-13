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
    SearchNearbyRequestDto requestDto = new SearchNearbyRequestDto(latitude, longitude);

    Map<String, String> PLACES_SEARCH_NEARBY_HEADERS = Map.of(
            "X-Goog-Api-Key", this.apiKey,
            "X-Goog-FieldMask", FIELD_MASK
    );

    NearbySearchResponse apiResponse = httpClientUtil.makePostRequestWithReturn(
            PLACES_SEARCH_NEARBY_URL, requestDto, NearbySearchResponse.class, null, PLACES_SEARCH_NEARBY_HEADERS
    );

    if (apiResponse != null && !apiResponse.getPlaces().isEmpty()) {
      NearbySearchResponse.Place place = apiResponse.getPlaces().get(0);
      log.info("Place found: {}", place.getDisplayName());
      address.setLocationName(place.getDisplayName().getText() != null ? place.getDisplayName().getText() : null);
      address.setLocationDescription(place.getEditorialSummary() != null ? place.getEditorialSummary().getText() : null);
      addressRepository.save(address);
      getPhotosFromPlace(address, place);
    }
  }

  private void getPhotosFromPlace(Address address, NearbySearchResponse.Place place) {
    if (place.getPhotos() != null && !place.getPhotos().isEmpty()) {
      NearbySearchResponse.Photo photo = place.getPhotos().get(0);
      String photoUrl = String.format(PLACES_MEDIA_URL, photo.getName(), apiKey);

      log.info("Fetching photo from URL: {}", photoUrl);

      HttpClient client = HttpClient.newBuilder()
              .followRedirects(HttpClient.Redirect.NEVER)
              .build();

      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(photoUrl))
              .GET()
              .build();

      HttpResponse<Void> response = null;

      try {
        response = client.send(request, HttpResponse.BodyHandlers.discarding());
        log.info("Response status code: {}", response.statusCode());

        if (response.statusCode() == 302) {
          log.info("header: {}", response.headers().firstValue("Location"));
          String imageUrl = response.headers().firstValue("Location").orElse(null);
          address.setPhotoUrl(imageUrl);
          addressRepository.save(address);
        } else {
          log.warn("No photos available for place ID: {}", place.getId());
        }
      } catch (IOException | InterruptedException e) {
        log.error("Error while fetching photo from place: {}", e.getMessage());
      }
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
