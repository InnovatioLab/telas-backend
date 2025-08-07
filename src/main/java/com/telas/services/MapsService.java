package com.telas.services;

import com.telas.entities.Address;

import java.util.Map;

public interface MapsService {
  void getAddressCoordinates(Address address);

  Map<String, Double> getCoordinatesFromZipCode(String zipCode, String countryCode);
}
