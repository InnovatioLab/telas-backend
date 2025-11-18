package com.telas.dtos.response;

import com.telas.entities.Monitor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
public final class BoxMonitorAdResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final List<MonitorAdResponseDto> ads;

  public BoxMonitorAdResponseDto(List<MonitorAdResponseDto> adLinks) {
    ads = adLinks;
  }
}