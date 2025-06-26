package com.telas.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StatusMonitorsResponseDto implements Serializable {
  @Serial
  private static final long serialVersionUID = 5288515525105234502L;

  private UUID id;

  private Boolean screenOn;

  private Boolean windowOpen;

  private Boolean fullScreen;

  private Boolean contentError;

  private Boolean errorStatusDelivery;

  private Boolean internetConnection;

  private String message;

  private String errorLevel;

  private Integer statusCode;
}
