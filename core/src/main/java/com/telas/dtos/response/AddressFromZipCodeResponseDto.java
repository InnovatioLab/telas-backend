package com.telas.dtos.response;

import com.telas.entities.Address;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class AddressFromZipCodeResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private final UUID id;

  private final String street;

  private final String zipCode;

  private final String city;

  private final String state;

  private final String country;

  private final Double latitude;

  private final Double longitude;

  public AddressFromZipCodeResponseDto(Address entity) {
    this.id = entity.getId();
    this.street = entity.getStreet();
    this.zipCode = entity.getZipCode();
    this.city = entity.getCity();
    this.state = entity.getState();
    this.country = entity.getCountry();
    this.latitude = entity.getLatitude();
    this.longitude = entity.getLongitude();
  }
}
