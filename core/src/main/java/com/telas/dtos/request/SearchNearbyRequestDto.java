package com.telas.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchNearbyRequestDto implements Serializable {
  private LocationRestriction locationRestriction;

  public SearchNearbyRequestDto(double latitude, double longitude) {
    this.locationRestriction = new LocationRestriction(new Circle(new Center(latitude, longitude), 100));
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationRestriction implements Serializable {
    private Circle circle;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Circle implements Serializable {
    private Center center;
    private int radius;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Center implements Serializable {
    private double latitude;
    private double longitude;
  }
}
