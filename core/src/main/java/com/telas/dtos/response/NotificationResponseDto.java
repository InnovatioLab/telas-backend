package com.telas.dtos.response;

import com.telas.entities.Notification;
import com.telas.enums.NotificationReference;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
public final class NotificationResponseDto implements Serializable {

  @Serial
  private static final long serialVersionUID = -8277181783061288942L;

  private final UUID id;

  private final NotificationReference reference;

  private final String message;

  private final String actionUrl;

  private final boolean visualized;

  public NotificationResponseDto(Notification entity) {
    id = entity.getId();
    reference = entity.getReference();
    message = entity.getMessage();
    actionUrl = entity.getActionUrl();
    visualized = entity.isVisualized();
  }
}
