package com.telas.dtos.response;

import com.telas.entities.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class CartItemResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private UUID id;

  private UUID monitorId;

  private Integer blockQuantity;

  private String monitorAddress;

  private Double latitude;

  private Double longitude;

  public CartItemResponseDto(CartItem entity) {
    id = entity.getId();
    monitorId = entity.getMonitor().getId();
    blockQuantity = entity.getBlockQuantity();
    monitorAddress = entity.getMonitor().getAddress().getCoordinatesParams();
    latitude = entity.getMonitor().getAddress().getLatitude();
    longitude = entity.getMonitor().getAddress().getLongitude();
  }
}
