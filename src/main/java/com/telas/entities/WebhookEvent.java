package com.telas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "webhook_events")
@NoArgsConstructor
public class WebhookEvent implements Serializable {
  @Serial
  private static final long serialVersionUID = 1084934057135367842L;

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "type")
  private String type;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  public WebhookEvent(String id, String type) {
    this.id = id;
    this.type = type;
    receivedAt = Instant.now();
  }
}
