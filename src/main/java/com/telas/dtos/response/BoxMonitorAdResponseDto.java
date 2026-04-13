package com.telas.dtos.response;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
public final class BoxMonitorAdResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = -2929124221854520175L;

  private final List<MonitorAdResponseDto> ads;

  public BoxMonitorAdResponseDto(List<MonitorAdResponseDto> adLinks) {
    ads = adLinks;
  }
}