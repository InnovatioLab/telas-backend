package com.telas.dtos.response;

import com.telas.entities.Wishlist;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
public final class WishlistResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -6368590854401119278L;

  private final UUID id;

  private final List<MonitorWishlistResponseDto> monitors;

  public WishlistResponseDto(Wishlist entity) {
    id = entity.getId();
    monitors = entity.getMonitors().stream()
            .map(MonitorWishlistResponseDto::new)
            .toList();
  }
}
